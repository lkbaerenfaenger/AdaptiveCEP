name := "goticks"

version := "1.0"

organization := "com.goticks"

libraryDependencies ++= {
  val akkaVersion = "2.4.9"
  Seq(
    "com.typesafe.akka" %% "akka-actor"                        % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core"                    % akkaVersion,
    "com.typesafe.akka" %% "akka-http-experimental"            % akkaVersion,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j"                        % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit"                      % akkaVersion % "test",
    "ch.qos.logback"    %  "logback-classic"                   % "1.1.3",
    "org.scalatest"     %% "scalatest"                         % "2.2.0"     % "test"
  )
}
