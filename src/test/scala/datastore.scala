import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration}
import org.scalatest.time.{Span, Seconds, Millis}
import org.scalatest._
import org.scalatest.concurrent._
import scala.util.{Failure, Success}
import datastore._
import json.JSON
import testhelpers.Helpers

package datastore {

  class DataStoreSpec extends FlatSpec with ScalaFutures with Matchers

  trait DataStoreBehaviors { this: DataStoreSpec =>
    def datastore(store: DataStore) {
      val doc = """{"a":"b"}"""
      def drop(table: String) = Helpers.blockingCall(store.drop(table))

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

      "A get" should "should return the doc in json" in {
        drop("get-happy")
        Helpers.blockingCall(store.put(doc, "get-happy"))
        val dbReturn = store.get(doc, "get-happy")
        whenReady(dbReturn) { dbOutput =>
          val filtered = JSON.filterFields(dbOutput, List("_id"))
          filtered shouldBe doc
        }
      }

      it should "return empty string if doc not found" in {
        drop("get-sad")
        val dbReturn = store.get(doc, "get-sad")
        whenReady(dbReturn) { dbOutput =>
          dbOutput shouldBe ""
        }
      }

    }
  }

  class MongoStoreSpec extends DataStoreSpec with DataStoreBehaviors {
    // Because Circle.ci is slow
    implicit val defaultPatience = PatienceConfig(
      timeout = Span(1, Seconds),
      interval = Span(25, Millis)
    )
    private val mongo = new MongoStore(
      "mongo://localhost:27017",
      "comicazi-mongostore-test"
    )
    it should behave like datastore(mongo)
  }

  class InMemoryStoreSpec extends DataStoreSpec with DataStoreBehaviors {
    it should behave like datastore(new InMemoryStore())
  }

}
