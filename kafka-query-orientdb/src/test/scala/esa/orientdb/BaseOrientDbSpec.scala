package esa.orientdb

import com.typesafe.scalalogging.StrictLogging
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpec}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.util.Try

abstract class BaseOrientDbSpec extends WordSpec with Matchers with Eventually with ScalaFutures with BeforeAndAfterAll with BeforeAndAfterEach with StrictLogging {

  def testTimeout: FiniteDuration = 7.seconds

  implicit override def patienceConfig =
    PatienceConfig(timeout = scaled(Span(testTimeout.toSeconds, Seconds)), interval = scaled(Span(150, Millis)))

  def orientDbEnv() = OrientDbEnv()

  type OrientDbClient
  private var latestClient = Option.empty[OrientDbClient]

  protected def orientDbClient = latestClient.getOrElse {
    val c = newClient
    latestClient = Option(c)
    c
  }

  private[this] val clients = ListBuffer[AutoCloseable]()

  def newClient(): OrientDbClient = {
    val c: AutoCloseable = ??? //OrientDbConnect("serviceUser", "changeTh1sDefaultPasswrd".toCharArray, databaseName, "orientDbdb://localhost:9010")
    clients += c
    ??? //c
  }

//  def orientDbDb: OrientDbDatabase = orientDbClient.getDatabase(databaseName)
//  def esaDb: EsaDB           = EsaDB(orientDbDb)

  def databaseName = "esa"

  private var orientDbIsRunningStateBeforeTest = false
  override def beforeAll(): Unit = {
    super.beforeAll()
    orientDbIsRunningStateBeforeTest = isOrientDbRunning()
    logger.warn(s"BeforeAll, orientDbIsRunningStateBeforeTest is $orientDbIsRunningStateBeforeTest...")
  }

  override def beforeEach(): Unit = {
    ensureOrientDbIsRunning() shouldBe true
  }

  override def afterAll(): Unit = {
    if (!orientDbIsRunningStateBeforeTest) {
      logger.warn("AfterAll stopping orientDb...")
      stopOrientDb()
    } else {
      logger.warn(s"AfterAll NOT stopping orientDb as !orientDbIsRunningStateBeforeTest is $orientDbIsRunningStateBeforeTest...")
    }
  }

  def isOrientDbRunning(): Boolean = BaseOrientDbSpec.Lock.synchronized {
    orientDbEnv.isOrientDbRunning()
  }

  def ensureOrientDbIsRunning(): Boolean = startOrientDb

//  def insertDocument[A: Encoder](collection: OrientDbCollection[Document], value: A) = {
//    import LowPriorityOrientDbImplicits._
//    val result = collection.insertOne(value).runAsyncGetFirst.futureValue
//    result shouldBe Some(Completed())
//    value
//  }

  def startOrientDb(): Boolean = BaseOrientDbSpec.Lock.synchronized {
    if (!isOrientDbRunning) {
      orientDbEnv.start()
      eventually {
        isOrientDbRunning() shouldBe true
      }
    }
    true
  }

  def restartOrientDb(): Boolean = {
    stopOrientDb() shouldBe true
    isOrientDbRunning() shouldBe false
    startOrientDb() shouldBe true
    val running = isOrientDbRunning()
    running shouldBe true
    running
  }

  def stopOrientDb(): Boolean = BaseOrientDbSpec.Lock.synchronized {
    Try(clients.foreach(_.close()))
    clients.clear()
    latestClient = None

    if (isOrientDbRunning) {
      orientDbEnv.stop()
      withClue("is running never returned false") {
        eventually {
          isOrientDbRunning() shouldBe false
        }
      }
    }
    true
  }
}

object BaseOrientDbSpec {
  private object Lock
}
