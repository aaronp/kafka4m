package kafkaquery.expresssions.expressions.spark

import example.Example
import kafkaquery.expressions.Expressions
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class SparkTest extends WordSpec with Matchers with BeforeAndAfterAll {

  "Expressions.cache" should {
    "allow filtering of data based on some text rule" in {
      val rdd: RDD[Example] = spark.sparkContext.parallelize(d8a)

      val ruleExpression: Broadcast[String] = spark.sparkContext.broadcast("""value.someInt > 80 && value.someInt < 100 || (value.someText == "allow")""")

      val filtered: RDD[Example] = rdd.mapPartitions { partition: Iterator[Example] =>
        val expr = Expressions.cache(ruleExpression.value)
        partition.filter(expr)
      }

      val found = filtered.collect()
      found should not be (empty)
      found.foreach { value =>
        withClue(value.toString) {
          val actual = value.getSomeInt > 80 && value.getSomeInt < 100 || (value.getSomeText == "allow")
          actual shouldBe true
        }
      }
    }
  }

  def d8a: Seq[Example] = {
    for {
      x    <- (10 to 50 by 10)
      y    <- (100 to 500 by 7)
      text <- Seq("allow", "don't allow", "meh")
    } yield {
      Example.newBuilder().setSomeInt(x).setSomeLong(y).setSomeText(text).build()
    }
  }

  private var spark: SparkSession = null

  override def beforeAll = {
    spark = SparkSession
      .builder()
      . //
      master("local[*]")
      . //
      appName(getClass.getSimpleName)
      . //
      config("spark.driver.bindAddress", "localhost")
      . //
      config("spark.driver.extraClassPath", ".")
      . //
      getOrCreate
  }

  override def afterAll = {
    spark.close()
  }
}
