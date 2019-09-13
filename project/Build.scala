import java.nio.file.Path

import sbt.IO

object Build {
  
  def docker(deployResourceDir: Path, //
             appAssembly: Path, //
             targetDir: Path, //
             logger: sbt.util.Logger) = {

    logger.info(
      s""" Building Docker Image with:
         |
         |   deployResourceDir = ${deployResourceDir.toAbsolutePath}
         |   appAssembly      = ${appAssembly.toAbsolutePath}
         |   targetDir         = ${targetDir.toAbsolutePath}
         |
       """.stripMargin)

    IO.copyDirectory(deployResourceDir.toFile, targetDir.toFile)
    IO.copy(List(appAssembly.toFile -> (targetDir.resolve("app.jar").toFile)))

    execIn(targetDir, "docker", "build", "--tag=kafka4m", ".")
  }


  def execIn(inDir: Path, cmd: String*): Unit = {
    import scala.sys.process._
    val p: ProcessBuilder = Process(cmd.toSeq, inDir.toFile)
    val retVal = p.!
    require(retVal == 0, cmd.mkString("", " ", s" in dir ${inDir} returned $retVal"))
  }

}
