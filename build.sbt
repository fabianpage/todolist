
name := "Todo"

version := "0.1.0"

scalaVersion := "2.10.1"

fork := true

scalacOptions += "-deprecation"

scalacOptions += "-feature"

javaOptions += "-XX:-PrintCompilation"

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "spray repo night" at "http://nightlies.spray.io"

resolvers += "Eligosource Releases" at "http://repo.eligotech.com/nexus/content/repositories/eligosource-releases"

resolvers += "Eligosource Snapshots" at "http://repo.eligotech.com/nexus/content/repositories/eligosource-snapshots"

libraryDependencies ++= Seq(
    "io.spray" % "spray-routing" % "1.2-M8",
    "io.spray" % "spray-can" % "1.2-M8",
    "io.spray" % "spray-httpx" % "1.2-M8",
    "io.spray" % "spray-http" % "1.2-M8",
    "io.spray" % "spray-io" % "1.2-M8",
    "io.spray" % "spray-util" % "1.2-M8",
    "io.spray" % "spray-testkit" % "1.2-M8",
    "io.spray" %% "spray-json" % "1.2.5",
    "org.scalaz" %% "scalaz-core" % "7.0.0",
    "com.typesafe.akka" %% "akka-actor" % "2.2.0-RC1",
    "com.typesafe.akka" %% "akka-slf4j" % "2.2.0-RC1",
    "com.typesafe.akka" %% "akka-testkit" % "2.2.0-RC1" % "test",
    "org.eligosource" %% "eventsourced-core" % "0.6-SNAPSHOT",
    "org.eligosource" %% "eventsourced-journal-journalio" % "0.6-SNAPSHOT",
    "org.eligosource" %% "eventsourced-journal-leveldb" % "0.6-SNAPSHOT",
    "org.eligosource" %% "eventsourced-journal-inmem" % "0.6-SNAPSHOT",
    "org.specs2" %% "specs2" % "2.0-RC2" % "test",
    "com.codahale.metrics" % "metrics-core" % "3.0.0-RC1"
)

//resolvers += "gseitz@github" at "http://gseitz.github.com/maven/"

//addSbtPlugin("com.github.gseitz" % "sbt-protobuf" % "0.2.2")

seq(Revolver.settings: _*)
