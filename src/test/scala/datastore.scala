import org.scalatest._
import org.scalatest.concurrent._
import datastore._

package datastore {

  class MongoStoreSpec extends FlatSpec with ScalaFutures with Matchers {
    private val mongo = new MongoStore(
      "localhost:27017",
      "comicazi-test"
    )
    val comic = "{\"publisher\": \"DC\", \"year\": 1973, \"mint\": true}"
    "A comic put" should "return the json of the comic" in {
      val dbReturn = mongo.put(comic, "comics")
      whenReady(dbReturn) { dbOutput =>
        dbOutput shouldBe comic
      }
    }
  }
}
