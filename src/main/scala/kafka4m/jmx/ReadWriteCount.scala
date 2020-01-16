package kafka4m.jmx

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import javax.management.ObjectName

import scala.util.Try

class ReadWriteCount(beanName: ObjectName) extends ReadWriteCountMBean with JMXSupport with AutoCloseable {
  protected val reads  = new Throughput.Builder
  protected val writes = new Throughput.Builder

  def incReads()   = readCounter.incrementAndGet()
  def incWrites()  = writeCounter.incrementAndGet()
  def readCounter  = reads.total
  def writeCounter = writes.total

  Try(registerAsMBean(beanName))

  override val getStarted = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)

  override def getReads(): Throughput = reads.flush()

  override def getWrites(): Throughput = writes.flush()

  override val getHealthy = true

  def description() = {
    s"""Reads: $getReads
       |Writes: $getWrites
       |""".stripMargin
  }

  override def close(): Unit = {
    Try(unregisterAsMBean(beanName))
  }
}
