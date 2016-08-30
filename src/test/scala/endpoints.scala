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
import schemas._
import json.Base64
import notification.NjWorker

package endpoints {

  class EndpointsSpec extends FlatSpec with ScalaFutures with Matchers {

    val comicJson = Helpers.ExampleComic.asJson()
    val subJson = Helpers.ExampleSubscription.asJson()
    val comicRequest = new HttpRequest(null, HttpBody(comicJson))
    val subRequest = new HttpRequest(null, HttpBody(subJson))

    "postComic" should "add the comic to the db" in {
      val db = new MongoStore("localhost:27017", "comicazi-test-endpoints-postComic1")
      val eps = new Endpoints(db)
      Helpers.blockingCall(db.drop("comics"))
      Helpers.blockingCall(eps.postComic(comicRequest))
      val dbReturn = db.get(comicJson, "comics")
      whenReady(dbReturn) { dbOutput =>
        dbOutput(0) shouldBe comicJson
        db.close()
      }
    }

    it should "create appropriate pendingqueries" in {
      val db = new MongoStore("localhost:27017", "comicazi-test-endpoints-postComic2")
      val eps = new Endpoints(db)
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
        db.close()
      }
    }

    it should "create a notificationjob" in {
      val db = new MongoStore("localhost:27017", "comicazi-test-endpoints-postComic3")
      val eps = new Endpoints(db)
      Helpers.blockingCall(db.drop("notificationjobs"))
      Helpers.blockingCall(eps.postComic(comicRequest))
      val dbReturn = db.get("{}", "notificationjobs")
      whenReady(dbReturn) { dbOutput =>
        val actualNj = new NotificationJob(dbOutput(0))
        val expectedNj = new NotificationJob(comicJson, 1)
        actualNj.toJson shouldBe expectedNj.toJson
        db.close()
      }
    }

    "postSubscription" should "create a subscription" in {
      val db = new MongoStore("localhost:27017", "comicazi-test-endpoints-postSubscription1")
      val eps = new Endpoints(db)
      Helpers.blockingCall(db.drop("subscriptions"))
      Helpers.blockingCall(eps.postSubscription(subRequest))
      val subReturn = db.get(subJson, "subscriptions")
      whenReady(subReturn) { dbOutput =>
        dbOutput(0) shouldBe subJson
        db.close()
      }
    }

    it should "create a querypattern" in {
      val db = new MongoStore("localhost:27017", "comicazi-test-endpoints-postSubscription2")
      val eps = new Endpoints(db)
      Helpers.blockingCall(db.drop("querypatterns"))
      Helpers.blockingCall(eps.postSubscription(subRequest))
      val sub = new Subscription(subJson)
      val qpJson = s"""{"querypattern":"${sub.querypattern}"}"""
      val qpReturn = db.get(qpJson, "querypatterns")
      whenReady(qpReturn) { dbOutput =>
        dbOutput(0) shouldBe qpJson
        db.close()
      }
    }

    "postSubscriptions+postComics" should "create notifications" in {
      val db = new MongoStore("localhost:27017", "comicazi-test-endpoints-end-to-end1")
      val eps = new Endpoints(db)
      Helpers.blockingCall(db.drop("subscriptions"))
      Helpers.blockingCall(db.drop("querypatterns"))
      Helpers.blockingCall(db.drop("pendingqueries"))
      Helpers.blockingCall(db.drop("notificationjobs"))
      Helpers.blockingCall(db.drop("pendingnotifications"))
      Helpers.blockingCall(db.drop("notifications"))
      val subscription1 = Helpers.ExampleSubscription.asJson
      val subscription2 = Helpers.ExampleSubscription2.asJson
      val subscription3 = Helpers.ExampleSubscription3.asJson
      val subscription4 = Helpers.ExampleSubscription4.asJson
      val comic1 = Helpers.ExampleComic.asJson
      val comic2 = Helpers.ExampleComic2.asJson
      List(subscription1, subscription2, subscription3, subscription4).map {
        case(sub: String) => {
          val newSubRequest = HttpRequest(null, HttpBody(sub))
          Helpers.blockingCall(eps.postSubscription(newSubRequest))
        }
      }
      List(comic1, comic2).map {
        case(comic: String) => {
          val newComicRequest = HttpRequest(null, HttpBody(comic))
          Helpers.blockingCall(eps.postComic(newComicRequest))
        }
      }
      val worker = new NjWorker(db, 1)
      Helpers.blockingCall(worker.lookForJob())
      Helpers.blockingCall(worker.lookForJob())
      val getNotifications = db.get("{}", "notifications")
      whenReady(getNotifications) { notifications =>
        notifications.length shouldBe 6 // until we have dedupe!
        db.close()
      }
    }

  }

}
