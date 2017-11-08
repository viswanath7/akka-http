import sbt.Resolver

name := "akka-http"

version := "0.1"

scalaVersion := "2.12.4"

resolvers += Resolver.typesafeRepo("releases")

libraryDependencies ++= Seq(
	"org.specs2" %% "specs2-core" % "3.8.6" % Test,
	"com.typesafe.akka" %% "akka-slf4j" % "2.4.19",
	"com.typesafe.akka" %% "akka-http" % "10.0.10",
	"com.typesafe.akka" %% "akka-http-spray-json" % "10.0.10",
	"ch.qos.logback" % "logback-classic" % "1.1.7",
	"org.scalactic" %% "scalactic" % "3.0.4",
	"com.typesafe.akka" %% "akka-testkit" % "2.4.19" % Test,
	"com.typesafe.akka" %% "akka-http-testkit" % "10.0.10" % Test,
	"org.scalatest" %% "scalatest" % "3.0.4" % Test
)

scalacOptions in Test ++= Seq("-Yrangepos")
