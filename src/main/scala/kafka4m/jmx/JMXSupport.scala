package kafka4m.jmx

import java.lang.management.ManagementFactory

import javax.management.{ObjectInstance, ObjectName}

import scala.reflect.ClassTag

/**
  * A mix-in trait which exposes a means to register a class as an MBean
  */
trait JMXSupport {

  protected def registerAsMBean(name: String = getClass.getSimpleName): ObjectInstance = {
    val mBeanName: ObjectName = JMXSupport.nameForClass(getClass, name)
    registerAsMBean(mBeanName)
  }

  protected def registerAsMBean(mBeanName: ObjectName): ObjectInstance = {
    val server = ManagementFactory.getPlatformMBeanServer
    server.registerMBean(this, mBeanName)
  }

  protected def unregisterAsMBean(mBeanName: ObjectName) = {
    ManagementFactory.getPlatformMBeanServer.unregisterMBean(mBeanName)
  }
}

object JMXSupport {
  def nameFor[A](implicit classTag: ClassTag[A]): ObjectName = {
    nameForClass(classTag.runtimeClass, classTag.runtimeClass.getSimpleName)
  }
  def nameFor[A](name: String)(implicit classTag: ClassTag[A]): ObjectName = {
    nameForClass(classTag.runtimeClass, name)
  }

  def nameForClass(c1ass: Class[_], name: String): ObjectName = {
    new ObjectName(s"${c1ass.getPackage.getName}:type=$name")
  }
}
