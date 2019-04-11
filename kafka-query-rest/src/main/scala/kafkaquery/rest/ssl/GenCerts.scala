package kafkaquery.rest.ssl

import java.nio.file.{Path, Paths}

import scala.collection.mutable.ArrayBuffer
import scala.sys.process
import scala.sys.process.ProcessLogger

/**
  * Utility for creating a certificate on a host. This is in 'main' (rather than just test) code, as we could potentially use this for other envs (staging, uat, etc)
  */
object GenCerts {

  def genCert(workDir: Path, certificateName: String, hostname: String, crtPassword: String, caPassword: String, jksPassword: String): (Int, BufferLogger, Path) = {
    val p12CertFile = workDir.resolve(certificateName)
    val (res, buffer) = run(
      "scripts/generateP12Cert.sh", //
      "CRT_DIR"           -> workDir.resolve("crt").toAbsolutePath.toString, //
      "CA_DIR"            -> workDir.resolve("ca").toAbsolutePath.toString, //
      "CRT_CERT_FILE_P12" -> p12CertFile.toAbsolutePath.toString, //
      "DNS_NAME"          -> hostname,
      "CRT_DEFAULT_PWD"   -> crtPassword,
      "CA_DEFAULT_PWD"    -> caPassword,
      "CRT_JKS_PW"        -> jksPassword
    )
    (res, buffer, p12CertFile)
  }

  def run(script: String, extraEnv: (String, String)*): (Int, BufferLogger) = {
    val buffer = new BufferLogger
    val res    = parseScript(script, extraEnv: _*).run(buffer)
    res.exitValue() -> buffer
  }

  private def parseScript(script: String, extraEnv: (String, String)*): process.ProcessBuilder = {
    val location = getClass.getClassLoader.getResource(script)
    require(location != null, s"Couldn't find $script")
    val scriptLoc = Paths.get(location.toURI)
    import sys.process._

    val scriptFile = s"./${scriptLoc.getFileName.toString}"
    Process(scriptFile, scriptLoc.getParent.toFile, extraEnv: _*)
  }

  class BufferLogger extends ProcessLogger {
    private val outputBuffer = ArrayBuffer[String]()
    private val errorBuffer  = ArrayBuffer[String]()
    private val bothBuffer   = ArrayBuffer[String]()

    def allOutput   = bothBuffer.mkString("\n")
    def stdOutput   = outputBuffer.mkString("\n")
    def errorOutput = errorBuffer.mkString("\n")

    override def out(s: => String): Unit = {
      outputBuffer.append(s)
      bothBuffer.append(s)
    }

    override def err(s: => String): Unit = {
      errorBuffer.append(s)
      bothBuffer.append(s)
    }

    override def buffer[T](f: => T): T = f
  }
}
