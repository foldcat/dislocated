val scala3Version = "3.3.3"
val PekkoVersion = "1.0.2"
val PekkoHttpVersion = "1.0.1"

lazy val maidlib = project
  .in(file("."))
  .settings(
    name := "maidlib",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,
    scalacOptions += "-feature",

    libraryDependencies += "org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion,
    libraryDependencies += "org.apache.pekko" %% "pekko-stream" % PekkoVersion,
    libraryDependencies += "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
    libraryDependencies += "org.typelevel" %% "fabric-core" % "1.14.3",
    libraryDependencies += "org.typelevel" %% "fabric-io" % "1.14.3",
    libraryDependencies += "org.slf4j" % "slf4j-api" % "2.0.13" % Test,
  )
