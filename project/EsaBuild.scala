import java.nio.file.Path

import scala.sys.process._
import sbt.IO

object EsaBuild {
  val MainRestClass = "esa.rest.RestServerMain"

  def docker(deployResourceDir: Path, //
             webResourceDir: Path, //
             restAssembly: Path, //
             targetDir: Path, //
             logger: sbt.util.Logger) = {
    logger.warn(s"dockerDir=$targetDir, mqttAssembly is ${restAssembly}")

    IO.copyDirectory(deployResourceDir.toFile, targetDir.toFile)
    IO.copy(List(restAssembly.toFile -> (targetDir.resolve("app.jar").toFile)))

    execIn(targetDir, "docker", "build", "--tag=esa", ".")
  }

  def execIn(inDir: Path, cmd: String*): Unit = {
    import scala.sys.process._
    val p: ProcessBuilder = Process(cmd.toSeq, inDir.toFile)
    val retVal            = p.!
    require(retVal == 0, cmd.mkString("", " ", s" in dir ${inDir} returned $retVal"))
  }

}
