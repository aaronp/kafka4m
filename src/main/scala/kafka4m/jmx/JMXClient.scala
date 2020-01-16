package kafka4m.jmx

import javax.management._
import javax.management.remote.{JMXConnectorFactory, JMXServiceURL}

import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag
import scala.util.Try

/**
  * Some utility over an [[MBeanServerConnection]]
  * @param server
  */
case class JMXClient(server: MBeanServerConnection) {

  def find[A: ClassTag](name: ObjectName): Option[Map[String, Try[AnyRef]]] = {
    server.queryMBeans(name, null).asScala.headOption.map { obj: ObjectInstance =>
      val info                                = server.getMBeanInfo(obj.getObjectName)
      val attributes: Seq[MBeanAttributeInfo] = info.getAttributes.toSeq
      val key                                 = ObjectName.getInstance(name)
      val attsByKey = attributes.map { attrInfo =>
        val value: Try[AnyRef] = attributeValue(key, attrInfo)
        attrInfo.getName -> value
      }
      attsByKey.toMap
    }
  }

  private def attributeValue(name: ObjectName, attr: MBeanAttributeInfo) = {
    Try(server.getAttribute(ObjectName.getInstance(name), attr.getName))
  }

}

object JMXClient {
  def forHostPort(hostPort: String = "localhost:5555"): JMXClient = {
    apply(s"service:jmx:rmi:///jndi/rmi://${hostPort}/jmxrmi")
  }

  def apply(serviceUrl: String): JMXClient = {
    val url                           = new JMXServiceURL(serviceUrl)
    val server: MBeanServerConnection = JMXConnectorFactory.connect(url).getMBeanServerConnection()
    new JMXClient(server)
  }
}
