val scala3Version    = "3.3.3"
val PekkoVersion     = "1.0.2"
val PekkoHttpVersion = "1.0.1"

lazy val maidlib = project
  .in(file("."))
  .settings(
    name    := "maidlib",
    version := "0.1.0-SNAPSHOT",
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked"
    ),
    scalaVersion := scala3Version,
    libraryDependencies += "org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion,
    libraryDependencies += "org.apache.pekko" %% "pekko-stream" % PekkoVersion,
    libraryDependencies += "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
    libraryDependencies += "org.slf4j"    % "slf4j-api" % "2.0.13" % Test,
    libraryDependencies += "com.lihaoyi" %% "upickle"   % "3.3.0"
  )
