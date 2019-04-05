package esa.mongo

import _root_.esa.mongo.esa._
import com.typesafe.scalalogging.StrictLogging
import io.circe.Encoder
import org.mongodb.scala.{Completed, Document, MongoClient, MongoCollection, MongoDatabase}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpec}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.util.Try

/**
  * A base class for mongo tests which ensures the database is running/connectable when tests are run, and shuts it down
  * at the end of the tests if it wasn't running when the tests started.
  *
  */
class BaseMongoSpec extends WordSpec with Matchers with Eventually with ScalaFutures with BeforeAndAfterAll with BeforeAndAfterEach with StrictLogging {

  def testTimeout: FiniteDuration = 7.seconds

  implicit override def patienceConfig =
    PatienceConfig(timeout = scaled(Span(testTimeout.toSeconds, Seconds)), interval = scaled(Span(150, Millis)))

  def mongoEnv() = MongoEnv()

  private var latestClient = Option.empty[MongoClient]

  protected def mongoClient = latestClient.getOrElse {
    val c = newClient
    latestClient = Option(c)
    c
  }

  private[this] val clients = ListBuffer[MongoClient]()

  def newClient(): MongoClient = {
    val c = MongoConnect("serviceUser", "changeTh1sDefaultPasswrd".toCharArray, databaseName, "mongodb://localhost:9010")
    clients += c
    c
  }

  def mongoDb: MongoDatabase = mongoClient.getDatabase(databaseName)
  def esaDb: EsaDB           = EsaDB(mongoDb)

  def databaseName = "esa"

  private var mongoIsRunningStateBeforeTest = false
  override def beforeAll(): Unit = {
    super.beforeAll()
    mongoIsRunningStateBeforeTest = isMongoRunning()
    logger.warn(s"BeforeAll, mongoIsRunningStateBeforeTest is $mongoIsRunningStateBeforeTest...")
  }

  override def beforeEach(): Unit = {
    ensureMongoIsRunning() shouldBe true
  }

  override def afterAll(): Unit = {
    if (!mongoIsRunningStateBeforeTest) {
      logger.warn("AfterAll stopping mongo...")
      stopMongo()
    } else {
      logger.warn(s"AfterAll NOT stopping mongo as !mongoIsRunningStateBeforeTest is $mongoIsRunningStateBeforeTest...")
    }
  }

  def isMongoRunning(): Boolean = BaseMongoSpec.Lock.synchronized {
    mongoEnv.isMongoRunning()
  }

  def ensureMongoIsRunning(): Boolean = startMongo

  def insertDocument[A: Encoder](collection: MongoCollection[Document], value: A) = {
    import LowPriorityMongoImplicits._
    val result = collection.insertOne(value).runAsyncGetFirst.futureValue
    result shouldBe Some(Completed())
    value
  }

  def startMongo(): Boolean = BaseMongoSpec.Lock.synchronized {
    if (!isMongoRunning) {
      mongoEnv.start()
      eventually {
        isMongoRunning() shouldBe true
      }
    }
    true
  }

  def restartMongo(): Boolean = {
    stopMongo() shouldBe true
    isMongoRunning() shouldBe false
    startMongo() shouldBe true
    val running = isMongoRunning()
    running shouldBe true
    running
  }

  def stopMongo(): Boolean = BaseMongoSpec.Lock.synchronized {
    Try(clients.foreach(_.close()))
    clients.clear()
    latestClient = None

    if (isMongoRunning) {
      mongoEnv.stop()
      withClue("is running never returned false") {
        eventually {
          isMongoRunning() shouldBe false
        }
      }
    }
    true
  }
}

object BaseMongoSpec {
  object Lock
}
