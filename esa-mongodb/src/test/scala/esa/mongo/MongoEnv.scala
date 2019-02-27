package esa.mongo
import scala.util.{Failure, Success, Try}

/**
  * Exposes a programmatic handle to start/stop the mongo db so integration tests can work.
  *
  * The other way we could do this would be to run the integration tests within e.g. a docker-compose setup.
  *
  * Doing it this way makes it a bit easier to develop/run locally
  *
  */
trait MongoEnv {
  /** @return true if we think mongo is running
    */
  def isMongoRunning(): Boolean

  /** @return true if this command succeeded, false otherwise
    */
  def start(): Boolean

  /** @return true if this command succeeded, false otherwise
    */
  def stop(): Boolean
}

object MongoEnv {
  def apply(): MongoEnv = DockerEnv

  object DockerEnv extends MongoEnv {
    override def isMongoRunning(): Boolean = {
      tryRunScript("scripts/startMongoDocker.sh").toOption.exists { output =>
        output.contains("is running")
      }
    }
    override def start(): Boolean          = tryRun("scripts/startMongoDocker.sh")
    override def stop(): Boolean           = tryRun("scripts/stopMongoDocker.sh")

    private def tryRun(script: String) = {
      println(script + " returned:")
      tryRunScript(script) match {
        case Success(output) =>
        println(output)
        true
        case Failure(output) =>
        println(output)
        false
      }
    }

    private def tryRunScript(script: String): Try[String] = Try(run(script))
  }

  def run(script: String): String = {
    import sys.process._
    script.!!
  }
}
