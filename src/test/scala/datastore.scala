import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration}
import org.scalatest.time.{Span, Seconds, Millis}
import org.scalatest._
import org.scalatest.concurrent._
import scala.util.{Failure, Success}
import datastore._
import json.Converter
import testhelpers.Helpers

package datastore {

  class MongoStoreSpec extends FlatSpec with ScalaFutures with Matchers {
    // Because Circle.ci is slow
    implicit val defaultPatience = PatienceConfig(
      timeout = Span(1, Seconds),
      interval = Span(25, Millis)
    )

    private val mongo = new MongoStore(
      "mongo://localhost:27017",
      "comicazi-mongostore-test"
    )
    val doc = """{"a":"b"}"""

    def drop(table: String) = Helpers.blockingCall(mongo.drop(table))

    "A put" should "return the json of the doc" in {
      drop("put-happy")
      val dbReturn = mongo.put(doc, "put-happy")
      whenReady(dbReturn) { dbOutput =>
        dbOutput shouldBe doc
      }
    }

    it should "fail for invalid json input" in {
      drop("put-sad")
      val dbReturn = mongo.put("{", "put-sad")
      whenReady(dbReturn.failed) { e =>
        e shouldBe a [Throwable]
      }
    }

    "A get" should "should return the doc in json" in {
      drop("get-happy")
      Helpers.blockingCall(mongo.put(doc, "get-happy"))
      val dbReturn = mongo.get(doc, "get-happy")
      whenReady(dbReturn) { dbOutput =>
        val filtered = Converter.filterFields(dbOutput, List("_id"))
        filtered shouldBe doc
      }
    }

    it should "return empty string if doc not found" in {
      drop("get-sad")
      val dbReturn = mongo.get(doc, "get-sad")
      whenReady(dbReturn) { dbOutput =>
        dbOutput shouldBe ""
      }
    }

  }
}
