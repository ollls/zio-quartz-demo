ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "3.2.1"

Runtime / unmanagedClasspath += baseDirectory.value / "src" / "main" / "resources"

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case _                                   => MergeStrategy.first
}

assembly / assemblyJarName := "qh2-http-run.jar"

lazy val root = (project in file(".")).settings(
  name := "json-template-qh2",
  libraryDependencies ++= Seq(
    "io.github.ollls" %% "zio-quartz-h2" % "0.5.4",
    "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.19.1",
    "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.19.1" % "compile-internal",
    "dev.zio" %% "zio-direct" % "1.0.0-RC7"
  )
)

scalacOptions ++= Seq(
  // "-Wunused:imports",
  // "-Xfatal-warnings",
  "-deprecation",
  "-feature",
  "-explain"
)
