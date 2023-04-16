ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "3.2.2"

Runtime / unmanagedClasspath += baseDirectory.value / "src" / "main" / "resources"

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case _                                   => MergeStrategy.first
}

assembly / assemblyJarName := "qh2-http-run.jar"

lazy val root = (project in file(".")).settings(
  name := "json-template-qh2",
  libraryDependencies ++= Seq(
    "io.github.ollls"                       %% "zio-quartz-h2"       % "0.5.4",
    "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.19.1",
    "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.19.1" % "compile-internal",
    "dev.zio" %% "zio-test"     % "2.0.12" % Test,
    "dev.zio" %% "zio-test-sbt" % "2.0.12" % Test
  )
)

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-explain"
)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
