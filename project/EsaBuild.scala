import java.nio.file.Path
import eie.io._
import scala.sys.process._
import sbt.IO

object EsaBuild {
  val MainRestClass = "esa.rest.RestServerMain"

  def docker(deployResourceDir: Path, //
             jsArtifacts: Seq[Path], //
             webResourceDir: Path, //
             restAssembly: Path, //
             targetDir: Path, //
             logger: sbt.util.Logger) = {

    logger.info(
      s""" Building Docker Image with:
         |
         |   deployResourceDir = ${deployResourceDir.toAbsolutePath}
         |   jsArtifacts       = ${jsArtifacts.map(_.toAbsolutePath).mkString(",")}
         |   webResourceDir    = ${webResourceDir.toAbsolutePath}
         |   restAssembly      = ${restAssembly.toAbsolutePath}
         |   targetDir         = ${targetDir.toAbsolutePath}
         |
       """.stripMargin)

    val esaJsDir = targetDir.resolve("web/js").mkDirs()
    IO.copyDirectory(deployResourceDir.toFile, targetDir.toFile)
    IO.copy(List(restAssembly.toFile -> (targetDir.resolve("app.jar").toFile)))
    IO.copy(jsArtifacts.map(jsFile => jsFile.toFile -> (esaJsDir.resolve(jsFile.fileName).toFile)))

    execIn(targetDir, "docker", "build", "--tag=esa", ".")
  }

  def execIn(inDir: Path, cmd: String*): Unit = {
    import scala.sys.process._
    val p: ProcessBuilder = Process(cmd.toSeq, inDir.toFile)
    val retVal            = p.!
    require(retVal == 0, cmd.mkString("", " ", s" in dir ${inDir} returned $retVal"))
  }

}
