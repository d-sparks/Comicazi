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

    // Because Circle.ci is slow
    implicit val defaultPatience = PatienceConfig(
      timeout = Span(1, Seconds),
      interval = Span(25, Millis)
    )

    def newDb() = new MongoStore(
      "mongodb://localhost:27017",
      "comicazi-mongostore-test"
    )
    def drop(db: MongoStore, table: String) = {
      Helpers.blockingCall(db.drop(table))
    }

    "A put" should "return the json of the doc" in {
      val db = newDb()
      drop(db, "put-happy")
      val dbReturn = db.put(doc, "put-happy")
      whenReady(dbReturn) { dbOutput =>
        dbOutput shouldBe doc
        db.close()
      }
    }

    it should "fail for invalid json input" in {
      val db = newDb()
      drop(db, "put-sad")
      val dbReturn = db.put("{", "put-sad")
      whenReady(dbReturn.failed) { e =>
        e shouldBe a [Throwable]
        db.close()
      }
    }

    "A put many" should "insert all docs" in {
      val db = newDb()
      drop(db, "put-many")
      Helpers.blockingCall(db.putMany(List(doc, doc), "put-many"))
      val dbReturn = db.get("{}", "put-many")
      whenReady(dbReturn) { dbOutput =>
        dbOutput shouldBe List(doc, doc)
        db.close()
      }
    }

    "A get" should "should return the doc in json" in {
      val db = newDb()
      drop(db, "get-happy")
      Helpers.blockingCall(db.put(doc2, "get-happy"))
      Helpers.blockingCall(db.put(doc, "get-happy"))
      Helpers.blockingCall(db.put(doc, "get-happy"))
      Helpers.blockingCall(db.put(doc, "get-happy"))
      val dbReturn = db.get(doc, "get-happy")
      whenReady(dbReturn) { dbOutput =>
        dbOutput shouldBe List(doc, doc, doc)
        db.close()
      }
    }

    it should "return empty list if doc not found" in {
      val db = newDb()
      drop(db, "get-sad")
      val dbReturn = db.get(doc, "get-sad")
      whenReady(dbReturn) { dbOutput =>
        dbOutput shouldBe List[String]()
        db.close()
      }
    }

    "A getN" should "return only n matching documents" in {
      val db = newDb()
      drop(db, "getN-happy")
      Helpers.blockingCall(db.put(doc2, "getN-happy"))
      Helpers.blockingCall(db.put(doc, "getN-happy"))
      Helpers.blockingCall(db.put(doc, "getN-happy"))
      Helpers.blockingCall(db.put(doc, "getN-happy"))
      val dbReturn = db.getN(2, doc, "getN-happy")
      whenReady(dbReturn) { dbOutput =>
        dbOutput shouldBe List(doc, doc)
        db.close()
      }
    }

    it should "return < n matches if not exhaustive" in {
      val db = newDb()
      drop(db, "getN-content")
      Helpers.blockingCall(db.put(doc2, "getN-content"))
      Helpers.blockingCall(db.put(doc, "getN-content"))
      val dbReturn = db.getN(2, doc, "getN-content")
      whenReady(dbReturn) { dbOutput =>
        dbOutput shouldBe List(doc)
        db.close()
      }
    }

    "A getLastN" should "return the newest n docs" in {
      val db = newDb()
      drop(db, "getLastN-happy")
      val doc1 = JSON.extend(doc, """{"a": "1"}""")
      val doc2 = JSON.extend(doc, """{"a": "2"}""")
      val doc3 = JSON.extend(doc, """{"a": "3"}""")
      Helpers.blockingCall(db.put(doc, "getLastN-happy"))
      Helpers.blockingCall(db.put(doc, "getLastN-happy"))
      val dbReturn = db.getLastN(2, doc, "getLastN-happy")
      whenReady(dbReturn) { dbOutput =>
        dbOutput shouldBe List(doc2, doc3)
        db.close()
      }
    }

  }

}
