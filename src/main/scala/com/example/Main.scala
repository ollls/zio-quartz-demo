package com.example

import zio.{ZIO, UIO, Task, Queue, Chunk, Promise, Ref, ExitCode, ZIOAppDefault}
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

//To re-generate slef-signed cert use.
//keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass password -validity 360 -keysize 2048
//in your browser: https://localhost:8443  ( click vist website, you need to accept slef-cigned cert first time )

//case class Device(id: Int, model: String)
//case class User(name: String, devices: Seq[Device])

object Main extends ZIOAppDefault {

  override val bootstrap =
    zio.Runtime.removeDefaultLoggers ++ SLF4J.slf4j ++ zio.Runtime.enableWorkStealing

  val CHAT_GPT_TOKEN = "YOUR-TOKEN"

  val TIMEOUT_MS = 60000

  val ctx = QuartzH2Client.buildSSLContext("TLSv1.3", null, null, true)

  case class ChatGPTMessage(role: String, content: String)
  case class ChatGPTAPIRequest(model: String, temperature: Float, messages: Array[ChatGPTMessage])

  given JsonValueCodec[ChatGPTAPIRequest] = JsonCodecMaker.make

  val R: HttpRouteIO[H2Client] =
    ///////////////////////////////////////
    case req @ GET -> Root / "test" => ZIO.attempt(Response.Ok())
    ///////////////////////////////////////
    case req @ POST -> Root / "token" =>
      for {
        text <- req.body.map(String(_))
        svc  <- ZIO.service[H2Client]

        conn <- svc.getConnection(req.connId)

        request <- ZIO.attempt(
          ChatGPTAPIRequest(
            "gpt-3.5-turbo",
            0.7,
            messages = Array(ChatGPTMessage("user", s"translate from English to Ukranian: '$text'"))
          )
        )

        response <- conn.doPost(
          "/v1/chat/completions",
          ZStream.fromIterable(writeToArray(request)),
          Headers().contentType(
            ContentType.JSON
          ) + ("Authorization" -> s"Bearer $CHAT_GPT_TOKEN")
        )
        output <- response.bodyAsText

      } yield (Response.Ok().contentType(ContentType.JSON).asText(output))

  def onDisconnect(id: Long) = (for {
    svc <- ZIO.service[H2Client]
    _   <- svc.close(id)
  } yield ())

  def onConnect(id: Long) = for {
    svc <- ZIO.service[H2Client]
    _ <- svc.open(id, "https://api.openai.com", TIMEOUT_MS, ctx = ctx, incomingWindowSize = 184590)
  } yield ()

  def run: Task[ExitCode] =
    for {
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

    } yield (exitCode)
}
