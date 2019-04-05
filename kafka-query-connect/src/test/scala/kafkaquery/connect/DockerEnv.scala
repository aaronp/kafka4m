package kafkaquery.connect

import java.nio.file.Paths

import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ArrayBuffer
import scala.sys.process
import scala.sys.process.ProcessLogger
import scala.util.{Failure, Success, Try}

/**
  * Represents a programmatic control over invoking start/stop/isRunning scripts to ensure our env is set up
  */
trait DockerEnv {

  /** @return true if we think Docker is running
    */
  def isDockerRunning(): Boolean

  /** @return true if this command succeeded, false otherwise
    */
  def start(): Boolean

  /** @return true if this command succeeded, false otherwise
    */
  def stop(): Boolean

  final def bracket[T](thunk: => T): T = {
    if (!isDockerRunning()) {
      start()
      try {
        thunk
      } finally {
        stop()
      }
    } else {
      thunk
    }
  }
}

object DockerEnv {

  def apply(scriptDir: String): DockerEnv = new Instance(scriptDir)

  class Instance(scriptDir: String) extends DockerEnv with LazyLogging {
    override def isDockerRunning(): Boolean = {
      tryRunScript(s"$scriptDir/isDockerRunning.sh").toOption.exists {
        case (_, output) =>
          output.contains("docker image ") && output.contains(" is running")
      }
    }

    override def start(): Boolean = tryRun(s"$scriptDir/startDocker.sh")

    override def stop(): Boolean = tryRun(s"$scriptDir/stopDocker.sh")

    private def tryRun(script: String): Boolean = tryRunScript(script).isSuccess

    private def tryRunScript(script: String): Try[(Int, String)] = {
      logger.debug(script + " returned:")
      Try(run(script)) match {
        case res @ Success(output) =>
          logger.debug(output.toString())
          res
        case res @ Failure(output) =>
          logger.debug(output.toString())
          res
      }
    }
  }

  def run(script: String): (Int, String) = {
    val buffer = new BufferLogger
    val res    = parseScript(script).run(buffer)
    res.exitValue() -> buffer.output
  }

  private def parseScript(script: String): process.ProcessBuilder = {
    val location = getClass.getClassLoader.getResource(script)
    require(location != null, s"Couldn't find $script")
    val scriptLoc = Paths.get(location.toURI)
    import sys.process._

    val scriptFile = s"./${scriptLoc.getFileName.toString}"
    Process(scriptFile, scriptLoc.getParent.toFile)
  }

  private class BufferLogger extends ProcessLogger {
    private val outputBuffer = ArrayBuffer[String]()

    def output = outputBuffer.mkString("\n")

    override def out(s: => String): Unit = {
      outputBuffer.append(s)
    }

    override def err(s: => String): Unit = {
      outputBuffer.append(s"ERR: $s")
    }

    override def buffer[T](f: => T): T = f
  }

}
