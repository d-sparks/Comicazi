import org.scalatest.time.{Span, Seconds, Millis}
import org.scalatest._
import org.scalatest.concurrent._
import datastore._
import json.Converter

package datastore {

  class MongoStoreSpec extends FlatSpec with ScalaFutures with Matchers {
    // Because Circle.ci is slow
    implicit val defaultPatience = PatienceConfig(
      timeout = Span(1, Seconds),
      interval = Span(25, Millis)
    )

    private val mongo = new MongoStore(
      "localhost:27017",
      "comicazi-test"
    )
    val doc = """{"a":"b"}"""

    "A put" should "return the json of the doc" in {
      val dbReturn = mongo.put(doc, "docs")
      whenReady(dbReturn) { dbOutput =>
        dbOutput shouldBe doc
      }
    }

    "A get" should "should return the doc in json" in {
      val dbReturn = mongo.get(doc, "docs")
      whenReady(dbReturn) { dbOutput =>
        val filtered = Converter.filterFields(dbOutput, List("_id"))
        filtered shouldBe doc
      }
    }

  }
}
