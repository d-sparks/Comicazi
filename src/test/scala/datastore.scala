import org.scalatest._
import org.scalatest.concurrent._
import datastore._
// note: if datastore can accept documents as strings, can avoid this import
import schemas._

package datastore {

  class MongoStoreSpec extends FlatSpec with ScalaFutures with Matchers {
    val comic = new Comic(
      "{\"publisher\": \"DC\", \"year\": 1973, \"mint\": true}"
    )

    "A put" should "return the json of a successful comic insert" in {
      val mongo = new MongoStore("localhost:27017", "comicazi-test")
      val dbReturn = mongo.put(comic, "comics")
      whenReady(dbReturn) { dbOutput =>
        dbOutput shouldBe comic.toJson()
      }
    }
  }
}
