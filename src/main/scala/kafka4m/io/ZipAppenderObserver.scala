package kafka4m.io

import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.zip.{ZipEntry, ZipOutputStream}

import com.typesafe.scalalogging.StrictLogging
import monix.execution.Ack
import monix.reactive.Observer

import scala.concurrent.Future

class ZipAppenderObserver(zipFile: Path, flushEvery: Int = 10, zipLevel: Int = -1) extends Observer[(String, Array[Byte])] with AutoCloseable with StrictLogging {

  require(flushEvery >= 0)
  if (!Files.exists(zipFile)) {
    Files.createFile(zipFile)
  }

  val zipOut = new ZipOutputStream(new FileOutputStream(zipFile.toFile), StandardCharsets.UTF_8)
  zipOut.setLevel(zipLevel)
  var flushCount = flushEvery

  override def onNext(elem: (String, Array[Byte])): Future[Ack] = {
    val (name, data) = elem

    val entry = new ZipEntry(name)
    zipOut.putNextEntry(entry)
    zipOut.write(data)
    zipOut.closeEntry()

    flushCount = flushCount - 1
    if (flushCount <= 0) {
      flushCount = flushEvery
      zipOut.flush()
    }

    Ack.Continue
  }

  override def onError(ex: Throwable): Unit = {
    logger.error(s"error: $ex ", ex)
    close()
  }

  override def onComplete(): Unit = {
    logger.info("onComplete")
    close()
  }

  override def close(): Unit = {
    logger.info("closing...")
    zipOut.flush()
    zipOut.close()
  }

}
