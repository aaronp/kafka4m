package esa.orientdb

import java.nio.file.Paths

import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ArrayBuffer
import scala.sys.process
import scala.sys.process.ProcessLogger
import scala.util.{Failure, Success, Try}

/**
  * Exposes a programmatic handle to start/stop the OrientDb so integration tests can work.
  *
  * The other way we could do this would be to run the integration tests within e.g. a docker-compose setup.
  *
  * Doing it this way makes it a bit easier to develop/run locally. This trait is just the programmatic equivalent
  * of just
  *
  * {{{
  *   cd esa-orientdb/src/test/resources/scripts
  * }}}
  *
  * and then running the following (several times, in any order to just play around w/ it):
  *
  * {{{
  *   cd esa-orientdb/src/test/resources/scripts
  *   ./startOrientDbDocker.sh
  *   ./stopOrientDbDocker.sh
  *   ./isOrientDbDockerRunning.sh
  * }}}
  *
  *
  * These are blocking calls, which is naughty for IO -- but all this is just for local test support, so we want to be
  * blocking on the environment anyway.
  */
trait OrientDbEnv {

  /** @return true if we think orientDb is running
    */
  def isOrientDbRunning(): Boolean

  /** @return true if this command succeeded, false otherwise
    */
  def start(): Boolean

  /** @return true if this command succeeded, false otherwise
    */
  def stop(): Boolean
}

object OrientDbEnv {
  def apply(): OrientDbEnv = DockerEnv

  object DockerEnv extends OrientDbEnv with LazyLogging {
    override def isOrientDbRunning(): Boolean = {
      tryRunScript("scripts/isOrientDbDockerRunning.sh").toOption.exists {
        case (_, output) =>
          output.contains("docker image ") && output.contains(" is running")
      }
    }
    override def start(): Boolean = tryRun("scripts/startOrientDbDocker.sh")
    override def stop(): Boolean  = tryRun("scripts/stopOrientDbDocker.sh")

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
    def output               = outputBuffer.mkString("\n")
    override def out(s: => String): Unit = {
      outputBuffer.append(s)
    }
    override def err(s: => String): Unit = {
      outputBuffer.append(s"ERR: $s")
    }
    override def buffer[T](f: => T): T = f
  }
}