import colossus._
import protocols.http._
import org.scalatest._
import org.scalatest.concurrent._
import scala.collection.mutable
import scala.collection.mutable.MutableList
import scala.concurrent.{Await, Future, Promise, ExecutionContext}
import ExecutionContext.Implicits.global
import datastore.MongoStore
import endpoints.Endpoints
import testhelpers.Helpers
import schemas.{Subscription, QueryPattern, PendingQuery}
import json.Base64

package endpoints {

  class EndpointsSpec extends FlatSpec with ScalaFutures with Matchers {

    val db = new MongoStore("localhost:27017", "comicazi-test-endpoints")
    val eps = new Endpoints(db)
    val comicJson = Helpers.ExampleComic.asJson()
    val subJson = Helpers.ExampleSubscription.asJson()
    val comicRequest = new HttpRequest(null, HttpBody(comicJson))
    val subRequest = new HttpRequest(null, HttpBody(subJson))

    "postComic" should "add the comic to the db" in {
      Helpers.blockingCall(db.drop("comics"))
      Helpers.blockingCall(eps.postComic(comicRequest))
      val dbReturn = db.get(comicJson, "comics")
      whenReady(dbReturn) { dbOutput =>
        dbOutput(0) shouldBe comicJson
      }
    }

    it should "create appropriate pendingqueries" in {
      Helpers.blockingCall(db.drop("pendingqueries"))
      Helpers.blockingCall(db.drop("querypatterns"))
      val qp = new QueryPattern("""{"querypattern": "publisher"}""")
      Helpers.blockingCall(db.put(qp.toJson, "querypatterns"))
      Helpers.blockingCall(eps.postComic(comicRequest))
      val dbReturn = db.get("{}", "pendingqueries")
      whenReady(dbReturn) { dbOutput =>
        val actualPq = new PendingQuery(dbOutput(0))
        val expectedPq = new PendingQuery("""{"publisher":"DC"}""", comicJson)
        actualPq.toJson shouldBe expectedPq.toJson
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
