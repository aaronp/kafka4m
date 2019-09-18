package kafka4m.io

import java.nio.file.Path

import cats.Show
import kafka4m.{Bytes, Key}
import org.apache.kafka.clients.consumer.ConsumerRecord

/**
  * Means to write data to zips
  */
object FileSink {

  /**
    * A class which can append entries to a file.
    *
    * Each entry will be a base64 line
    *
    * @param file the zip file
    * @return an observer which writes base64 encoded entries to the given file
    */
  def base64(file: Path, flushEvery: Int = 10): TextAppenderObserver[ConsumerRecord[Key, Bytes]] = {
    text(file, flushEvery)(TextAppenderObserver.ShowRecord)
  }

  def text[A: Show](file: Path, flushEvery: Int): TextAppenderObserver[A] = new TextAppenderObserver[A](file, flushEvery)

  /**
    * Writes the data from the observer into a zip file
    *
    * @param zipFile the zip file
    * @return
    */
  def zipped(zipFile: Path, flushEvery: Int = 10, zipLevel: Int = -1): ZipAppenderObserver[(Key, Bytes)] = {
    new ZipAppenderObserver(zipFile, flushEvery, zipLevel)
  }
}
