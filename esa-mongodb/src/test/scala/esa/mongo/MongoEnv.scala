package esa.mongo
import java.nio.file.Paths

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
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
      tryRunScript("scripts/startMongoDocker.sh").toOption.exists {
        case (_, output) => output.contains("is running")
      }
    }
    override def start(): Boolean = tryRun("scripts/startMongoDocker.sh")
    override def stop(): Boolean  = tryRun("scripts/stopMongoDocker.sh")

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

    private def tryRunScript(script: String): Try[(Int, String)] = Try(run(script))
  }

  def run(script: String): (Int, String) = {

    val location  = getClass.getResource(script)
    val scriptLoc = Paths.get(location.toURI)
    import sys.process._

    val proc = Process(scriptLoc.getFileName.toString, scriptLoc.getParent.toFile)
    object Logger extends ProcessLogger {
      private val outputBuffer = ArrayBuffer[String]()
      def output               = outputBuffer.mkString("\n")
      override def out(s: => String): Unit = {
        outputBuffer.append(s)
      }
      override def err(s: => String): Unit = {
        outputBuffer.append(s"ERR: $s")
      }
      override def buffer[T](f: => T): T = f
    }
    val res: Process = proc.run(Logger)
    res.exitValue() -> Logger.output
  }
}
