import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration}
import org.scalatest.time.{Span, Seconds, Millis}
import org.scalatest._
import org.scalatest.concurrent._
import scala.util.{Failure, Success}
import datastore._
import helpers.JSON
import testhelpers.Helpers

package datastore {

  class MongoStoreSpec extends FlatSpec with ScalaFutures with Matchers {
    val doc = """{"a":"b"}"""
    val doc2 = """{"c":"d"}"""

    def drop(table: String) = Helpers.blockingCall(store.drop(table))

    // Because Circle.ci is slow
    implicit val defaultPatience = PatienceConfig(
      timeout = Span(1, Seconds),
      interval = Span(25, Millis)
    )
    private val store = new MongoStore(
      "mongodb://localhost:27017",
      "comicazi-mongostore-test"
    )

    "A put" should "return the json of the doc" in {
      drop("put-happy")
      val dbReturn = store.put(doc, "put-happy")
      whenReady(dbReturn) { dbOutput =>
        dbOutput shouldBe doc
      }
    }

    it should "fail for invalid json input" in {
      drop("put-sad")
      val dbReturn = store.put("{", "put-sad")
      whenReady(dbReturn.failed) { e =>
        e shouldBe a [Throwable]
      }
    }

    "A put many" should "insert all docs" in {
      drop("put-many")
      Helpers.blockingCall(store.putMany(List(doc, doc), "put-many"))
      val dbReturn = store.get("{}", "put-many")
      whenReady(dbReturn) { dbOutput =>
        dbOutput shouldBe List(doc, doc)
      }
    }

    "A get" should "should return the doc in json" in {
      drop("get-happy")
      Helpers.blockingCall(store.put(doc2, "get-happy"))
      Helpers.blockingCall(store.put(doc, "get-happy"))
      Helpers.blockingCall(store.put(doc, "get-happy"))
      Helpers.blockingCall(store.put(doc, "get-happy"))
      val dbReturn = store.get(doc, "get-happy")
      whenReady(dbReturn) { dbOutput =>
        dbOutput shouldBe List(doc, doc, doc)
      }
    }

    it should "return empty list if doc not found" in {
      drop("get-sad")
      val dbReturn = store.get(doc, "get-sad")
      whenReady(dbReturn) { dbOutput =>
        dbOutput shouldBe List[String]()
      }
    }

    "A getN" should "return only n matching documents" in {
      drop("getN-happy")
      Helpers.blockingCall(store.put(doc2, "getN-happy"))
      Helpers.blockingCall(store.put(doc, "getN-happy"))
      Helpers.blockingCall(store.put(doc, "getN-happy"))
      Helpers.blockingCall(store.put(doc, "getN-happy"))
      val dbReturn = store.getN(2, doc, "getN-happy")
      whenReady(dbReturn) { dbOutput =>
        dbOutput shouldBe List(doc, doc)
      }
    }

    it should "return < n matches if not exhaustive" in {
      drop("getN-content")
      Helpers.blockingCall(store.put(doc2, "getN-content"))
      Helpers.blockingCall(store.put(doc, "getN-content"))
      val dbReturn = store.getN(2, doc, "getN-content")
      whenReady(dbReturn) { dbOutput =>
        dbOutput shouldBe List(doc)
      }
    }

  }

}
