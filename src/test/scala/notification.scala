import akka.actor.{ActorSystem, Props}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.{Future, Promise}
import scala.collection.mutable.MutableList
import datastore.MongoStore
import notification.NjWorker
import schemas.{NotificationJob, PendingQuery, Subscription, Comic}
import testhelpers.Helpers
import helpers.{JSON, Base64}

package notification {

  class NotificationSpec extends FlatSpec with Matchers

  class NjWorkerSpec extends NotificationSpec with ScalaFutures {
    implicit val sys = ActorSystem()

    val db = new MongoStore("localhost:27017", "comicazi-test-notification")

    val njWorker = new NjWorker(db, 1)

    // Note: Using traits and `should behave like`, one could have very
    // beautiful tests which test that each concurrent step behaves as
    // intended and "does the rest", but it would cost a decent amount of work.

    val subscription = new Subscription(
      Helpers.ExampleSubscription.asJson
    )
    val comic = new Comic(
      Helpers.ExampleComic.asJson
    )

    def dropFakeData() {
      Helpers.blockingCall(db.drop("subscriptions"))
      Helpers.blockingCall(db.drop("querypatterns"))
      Helpers.blockingCall(db.drop("pendingqueries"))
      Helpers.blockingCall(db.drop("pendingnotifications"))
      Helpers.blockingCall(db.drop("notifications"))
    }
    def setUpFakeData() {
      dropFakeData()
      Helpers.blockingCall(db.put(subscription.toJson, "subscriptions"))
      val querystring = JSON.project(comic.toJson, List("publisher"))
      val pq = new PendingQuery(querystring, comic.toJson)
      Helpers.blockingCall(db.put(pq.toJson(), "pendingqueries"))
      val nj = new NotificationJob(comic.toJson, 1)
      Helpers.blockingCall(db.put(nj.toJson(), "notificationjobs"))
    }

    "look for a job" should "make notifications" in {
      setUpFakeData()
      Helpers.blockingCall(njWorker.lookForJob())
      val pns = db.get("{}", "notifications")
      whenReady(pns) { results =>
        results.length shouldBe 1
      }
    }

    it should "not leave pending queries behind" in {
      setUpFakeData()
      Helpers.blockingCall(njWorker.lookForJob())
      val pqs = db.get("{}", "pendingqueries")
      whenReady(pqs) { results =>
        results shouldBe List[String]()
      }
    }

    it should "not create pendingnotifications unless ∃ pendingqueries" in {
      setUpFakeData()
      Helpers.blockingCall(db.remove("{}", "pendingqueries"))
      Helpers.blockingCall(njWorker.lookForJob())
      val pns = db.get("{}", "pendingnotifications")
      whenReady(pns) { results =>
        results shouldBe List[String]()
      }
    }

    "checkPqsSuccess" should "fail if any PendingQuery failed" in {
      val f = njWorker.checkPqsSuccess(comic)(List(true, false, true))
      whenReady(f.failed) { e =>
        e shouldBe a [Throwable]
        e.getMessage shouldBe "A query pattern failed"
      }
    }

    "checkPqsExhaustive" should "fail if ∃ PendingQueries" in {
      setUpFakeData()
      val f = njWorker.checkPqsExhaustive(comic)()
      whenReady(f.failed) { e =>
        e shouldBe a [Throwable]
        e.getMessage shouldBe "PendingQuery remaining"
      }
    }

  }

}
