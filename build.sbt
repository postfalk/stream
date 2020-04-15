name := """stream"""

organization := "org.tnc.casoftwaredev"

version := "0.7-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(guice, jdbc, ws)
//libraryDependencies += "com.h2database" % "h2" % "1.4.200"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
libraryDependencies += "com.typesafe.play" %% "play-iteratees" % "2.6.1"
libraryDependencies += "com.typesafe.play" %% "play-iteratees-reactive-streams" % "2.6.1"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.1"
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "1.1.2"
// libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "0.18"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.6.1"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.6.1"
//libraryDependencies += "com.typesafe.play" % "play-jdbc-evolutions_2.12" % "2.6.1"
//libraryDependencies ++= Seq(
//  "org.postgresql" % "postgresql" % "42.2.8",
//  "io.getquill" %% "quill-jdbc" % "3.5.0"
//)
javaOptions in Test += "-Dconfig.file=conf/application.test.conf"
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
// routesGenerator := InjectedRoutesGenerator
