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
import schemas.Subscription

package endpoints {

  class EndpointsSpec extends FlatSpec with ScalaFutures with Matchers {

    val db = new InMemoryStore()
    val eps = new Endpoints(db)
    val comicJson = Helpers.ExampleComic.asJson()
    val subJson = Helpers.ExampleSubscription.asJson()
    val comicRequest = new HttpRequest(null, HttpBody(comicJson))
    val subRequest = new HttpRequest(null, HttpBody(subJson))

    "postComic" should "succeed for new comic" in {
      Helpers.blockingCall(db.drop("comics"))
      Helpers.blockingCall(eps.postComic(comicRequest))
      // If the API had a get method, would prefer to test post/get
      val dbReturn = db.get(comicJson, "comics")
      whenReady(dbReturn) { dbOutput =>
        dbOutput(0) shouldBe comicJson
      }
    }

    "postSubscription" should "create a subscription" in {
      Helpers.blockingCall(db.drop("subscriptions"))
      Helpers.blockingCall(eps.postSubscription(subRequest))
      val subReturn = db.get(subJson, "subscriptions")
      whenReady(subReturn) { dbOutput =>
        dbOutput(0) shouldBe subJson
      }
    }

    it should "create a querypattern" in {
      Helpers.blockingCall(db.drop("querypatterns"))
      Helpers.blockingCall(eps.postSubscription(subRequest))
      val sub = new Subscription(subJson)
      val qpJson = s"""{"querypattern":"${sub.querypattern}"}"""
      val qpReturn = db.get(qpJson, "querypatterns")
      whenReady(qpReturn) { dbOutput =>
        dbOutput(0) shouldBe qpJson
      }
    }


  }

}
