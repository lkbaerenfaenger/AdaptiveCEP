name := "EventScala"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.11.8",
  "com.typesafe.akka" %% "akka-actor" % "2.4.9",
  "com.espertech" % "esper" % "5.5.0"
)
