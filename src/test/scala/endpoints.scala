import colossus._
import protocols.http._
import org.scalatest._
import org.scalatest.concurrent._
import scala.collection.mutable
import scala.collection.mutable.MutableList
import scala.concurrent.{Await, Future, Promise, ExecutionContext}
import ExecutionContext.Implicits.global
import datastore.InMemoryStore
import endpoints.Endpoints
import testhelpers.Helpers

package endpoints {

  class EndpointsSpec extends FlatSpec with ScalaFutures with Matchers {

    val db = new InMemoryStore()
    val eps = new Endpoints(db)
    val comicJson = Helpers.ExampleComic.asJson()
    val request = new HttpRequest(null, HttpBody(comicJson))

    "postComic" should "succeed for new comic" in {
      Helpers.blockingCall(db.drop("comics"))
      Helpers.blockingCall(eps.postComic(request))
      // If the API had a get method, would prefer to test post/get
      val dbReturn = db.get(comicJson, "comics")
      whenReady(dbReturn) { dbOutput =>
        dbOutput shouldBe comicJson
      }
    }

    it should "fail for an existing comic" in {
      Helpers.blockingCall(db.drop("comics"))
      Helpers.blockingCall(eps.postComic(request))
      val endpointReturn = eps.postComic(request)
      whenReady(endpointReturn.failed) { e =>
        e shouldBe an [Exception]
      }
    }

  }

}
