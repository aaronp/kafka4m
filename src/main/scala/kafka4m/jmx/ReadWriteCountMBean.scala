package kafka4m.jmx

import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import javax.management.ObjectName

import scala.util.Try
import scala.util.control.NonFatal

/**
  * An mbean which counts reads/writes
  */
trait ReadWriteCountMBean {

  /**
    * The application start time
    * @return the application start time as an ISO date time
    */
  def getStarted(): String

  /** @return a simple yes/no response for use as a health endpoint
    */
  def getHealthy(): Boolean

  /**
    * NOTE: $SIDEEFFECT
    * @return the current read throughput
    */
  def getReads(): Throughput

  /**
    * NOTE: $SIDEEFFECT
    * @return the current read throughput
    */
  def getWrites(): Throughput
}

object ReadWriteCountMBean extends StrictLogging {

  def apply(rootConfig: Config): Remote = {
    val serviceUrl: String = rootConfig.getString("kafka4m.jmx.client.serviceUrl")
    logger.info(s"Connecting to $serviceUrl")
    val mbeanName: ObjectName = ObjectName.getInstance(rootConfig.getString("kafka4m.jmx.client.mbeanName"))
    remote(JMXClient(serviceUrl), mbeanName)
  }

  def pretty(bean: ReadWriteCountMBean): String = {
    s"""
       |started at ${bean.getStarted()}
       |${if (bean.getHealthy()) " is healthy" else "isn't well"}
       |Reads:
       |${bean.getReads()}
       |
       |Writes:
       |${bean.getWrites()}
       |""".stripMargin
  }

  def remote(client: JMXClient, mbeanName: ObjectName): Remote = Remote(client, mbeanName)

  case class Remote(client: JMXClient, mbeanName: ObjectName) extends ReadWriteCountMBean {
    val attMap = client.find[ReadWriteCountMBean](mbeanName)
    private def unsafe[A](key: String) = {
      try {
        val opt: Option[Try[AnyRef]] = attMap.flatMap(_.get(key))
        opt.get.get.asInstanceOf[A]
      } catch {
        case NonFatal(e) =>
          sys.error(s"Error reading '$key' from '$mbeanName' => ${attMap} : $e")
      }
    }

    override def getStarted(): String    = unsafe("Started")
    override def getHealthy(): Boolean   = unsafe("Healthy")
    override def getReads(): Throughput  = unsafe("Reads")
    override def getWrites(): Throughput = unsafe("Writes")
  }
}
