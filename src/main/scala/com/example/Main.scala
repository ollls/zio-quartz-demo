package com.example

import zio.{ZIO, UIO, Task, Queue, Chunk, Promise, Ref, ExitCode, ZIOAppDefault}
import zio.stream.{ZSink, ZStream}
import io.quartz.QuartzH2Server
import io.quartz.QuartzH2Client
import io.quartz.http2.routes.{HttpRouteIO, Routes}
import io.quartz.http2.model.{Headers, Method, ContentType, Request, Response}
import io.quartz.http2._
import io.quartz.http2.model.Method._
import io.quartz.http2.model.ContentType.JSON
import zio.logging.backend.SLF4J

import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._
import zio.stream.ZStream
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.ConcurrentMapHasAsScala
import io.quartz.services.H2Client
import ch.qos.logback.classic.Level
import zio.direct._

case class ChatGPTMessage(role: String, content: String)
case class ChatGPTAPIRequest(model: String, temperature: Float, messages: Array[ChatGPTMessage])

given JsonValueCodec[ChatGPTAPIRequest] = JsonCodecMaker.make

//To re-generate slef-signed cert use.
//keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass password -validity 360 -keysize 2048
//in your browser: https://localhost:8443  ( click vist website, you need to accept slef-cigned cert first time )

object Main extends ZIOAppDefault {

  val CHAT_GPT_TOKEN = "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"

  override val bootstrap =
    zio.Runtime.removeDefaultLoggers ++ SLF4J.slf4j ++ zio.Runtime.enableWorkStealing

  val TIMEOUT_MS = 60000

  val ctx = QuartzH2Client.buildSSLContext("TLSv1.3", null, null, true)

  val R: HttpRouteIO[H2Client] =
    ///////////////////////////////////////
    case req @ GET -> Root / "test" =>
      defer {
        Response.Ok()
      }

    case req @ POST -> Root / "upload" / StringVar(file) =>
      defer {
        val FOLDER_PATH = "/Users/ostrygun/web_root/"
        val FILE        = s"$file"
        val path        = new java.io.File(FOLDER_PATH + FILE)
        val u           = req.stream.run(ZSink.fromFile(path)).run
        Response.Ok().asText(s"OK $u")
      }

    ///////////////////////////////////////
    case req @ POST -> Root / "token" =>
      defer {
        val input      = req.body.map(String(_)).run
        val svc        = ZIO.service[H2Client].run
        val connection = svc.getConnection(req.connId).run
        // Detected the use of a mutable collection inside a defer clause (called: Array).
        // Mutable collections can cause many potential issues as a result of defer-clause
        // rewrites so they are not allowed (Unless it is inside of a run-call).
        val request = ZIO
          .succeed(
            ChatGPTAPIRequest(
              "gpt-3.5-turbo",
              0.7,
              messages =
                Array(ChatGPTMessage("user", s"translate from English to Ukranian: '$input'"))
            )
          )
          .run // we did run call to address the above error

        val response = connection
          .doPost(
            "/v1/chat/completions",
            ZStream.fromIterable(writeToArray(request)),
            Headers().contentType(
              ContentType.JSON
            ) + ("Authorization" -> s"Bearer $CHAT_GPT_TOKEN")
          )
          .run
        val output = response.bodyAsText.run
        Response.Ok().contentType(ContentType.JSON).asText(output)
      }

  def onDisconnect(id: Long): ZIO[H2Client, Nothing, Unit] = defer {
    val svc = ZIO.service[H2Client].run
    svc.close(id).run
  }

  def onConnect(id: Long) = defer {
    val svc = ZIO.service[H2Client].run
    svc.open(id, "https://api.openai.com", TIMEOUT_MS, ctx = ctx, incomingWindowSize = 184590).run
  }

  def run =
    val env = zio.ZLayer.fromZIO(ZIO.succeed("Hello ZIO World!"))
    (for {

      args <- this.getArgs

      _ <- ZIO.when(args.find(_ == "--debug").isDefined)(
        ZIO.attempt(QuartzH2Server.setLoggingLevel(Level.DEBUG))
      )
      _ <- ZIO.when(args.find(_ == "--error").isDefined)(
        ZIO.attempt(QuartzH2Server.setLoggingLevel(Level.ERROR))
      )
      _ <- ZIO.when(args.find(_ == "--off").isDefined)(
        ZIO.attempt(QuartzH2Server.setLoggingLevel(Level.OFF))
      )

      ctx <- QuartzH2Server.buildSSLContext("TLS", "keystore.jks", "password")
      exitCode <- new QuartzH2Server[H2Client](
        "localhost",
        8443,
        TIMEOUT_MS,
        ctx,
        onConnect = onConnect,
        onDisconnect = onDisconnect
      )
        .startIO(R, sync = false)
        .provide(H2Client.layer)

    } yield (exitCode)).provideSomeLayer(env)
}
