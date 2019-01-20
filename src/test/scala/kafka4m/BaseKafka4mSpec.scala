package kafka4m

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

abstract class BaseKafka4mSpec extends WordSpec with Matchers with ScalaFutures {}
