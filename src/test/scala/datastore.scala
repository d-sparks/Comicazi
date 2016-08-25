import org.scalatest._
import org.scalatest.concurrent._
import datastore._

package datastore {

  class MongoStoreSpec extends FlatSpec with ScalaFutures with Matchers {
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
  }
}
