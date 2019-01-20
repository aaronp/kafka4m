import sbt._

object Dependencies {

  val config: ModuleID = "com.typesafe" % "config" % "1.3.0"

  //https://github.com/typesafehub/scala-logging
  val logging = List(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
    "ch.qos.logback"                                % "logback-classic" % "1.1.11")

  val testDependencies = List(
    "org.scalactic" %% "scalactic" % "3.0.5"   % "test",
    "org.scalatest" %% "scalatest" % "3.0.5"   % "test",
    "org.pegdown"                  % "pegdown" % "1.6.0" % "test",
    "junit"                        % "junit"   % "4.12" % "test"
  )

  val monix = List("monix", "monix-execution", "monix-eval", "monix-reactive", "monix-tail").map { art =>
    "io.monix" %% art % "3.0.0-RC1"
  }

  val simulacrum: ModuleID = "com.github.mpilquist" %% "simulacrum" % "0.13.0"

  
  val endpointsServer = List(
      "org.julienrf" %% "endpoints-play-server-circe" % "0.8.0",
      "org.julienrf" %% "endpoints-openapi" % "0.8.0",
      "org.scala-stm" %% "scala-stm" % "0.8")
 
  val esaRest: List[ModuleID] = config :: logging ::: testDependencies ::: endpointsServer
  val esaRender: List[ModuleID] = ("es.nitaur.markdown" % "txtmark" % "0.16") +: esaRest

  val esaDB: List[ModuleID] = ("org.scalaz" %% "scalaz-zio" % "0.5.3") +: 
    ("com.michaelpollmeier" %% "gremlin-scala" % "3.3.4.15") +: 
    esaRest
}
