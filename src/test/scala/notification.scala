import akka.actor.{ActorSystem, Props}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Span, Seconds, Millis}
import scala.concurrent.{Future, Promise} // ExecutionContext
import scala.collection.mutable.MutableList
import datastore.MongoStore
import notification.NjWorker
import schemas.{NotificationJob, PendingQuery}
import testhelpers.Helpers
import json.{JSON, Base64}

package notification {

  class NotificationSpec extends FlatSpec with Matchers

  class NjWorkerSpec extends NotificationSpec with ScalaFutures {
     implicit val sys = ActorSystem()
     implicit val defaultPatience = PatienceConfig(
       timeout = Span(10, Seconds),
       interval = Span(50, Millis)
     )

    val db = new MongoStore("localhost:27017", "comicazi-test-notification")

    val njWorker = new NjWorker(db, 1)

    "sequentially" should "perform all of its tasks" in {
      var calls = 0
      val lambdas = (1 to 100).map({case(_) =>
        {() => {
          calls += 1
          val p = Promise[Unit]
          p.success()
          p.future
        }}
      }).toList
      val p = Promise[List[Boolean]]
      njWorker.sequentially[Unit](p, 0, lambdas, List[Boolean]())
      whenReady(p.future) { results =>
        calls shouldBe 100
        results.length shouldBe 100
        results.contains(false) shouldBe false
      }
    }

    it should "detect exceptions, continue, and operate in order" in {
      val e = new Throwable("")
      val f1 = () => {val p = Promise[Unit]; p.failure(e); p.future}
      val f2 = () => {val p = Promise[Unit]; p.success(); p.future}
      val p = Promise[List[Boolean]]
      njWorker.sequentially(p, 0, List(f1, f2), List[Boolean]())
      whenReady(p.future) { results =>
        results shouldBe List(false, true)
      }
    }

    it should "work for large tasks lists" in {
      val lambdas = (1 to 10000).map({case(_) =>
        () => {val p = Promise[Unit]; p.success(); p.future}
      }).toList
      val p = Promise[List[Boolean]]
      njWorker.sequentially(p, 0, lambdas, List[Boolean]())
      whenReady(p.future) { results =>
        results.length shouldBe 10000
      }
    }

    // Note: Using traits and `should behave like`, one could have very
    // beautiful tests which test that each concurrent step behaves as
    // intended and "does the rest", but it would cost a decent amount of work.

    def dropFakeData() {
      Helpers.blockingCall(db.drop("subscriptions"))
      Helpers.blockingCall(db.drop("querypatterns"))
      Helpers.blockingCall(db.drop("pendingqueries"))
      Helpers.blockingCall(db.drop("pendingnotifications"))
    }
    def setUpFakeData() {
      dropFakeData()
      Helpers.blockingCall(db.put(
        Helpers.ExampleSubscription.asJson(),
        "subscriptions"
      ))
      val comicJson = Helpers.ExampleComic.asJson()
      val querystring = JSON.project(comicJson, List("publisher"))
      val pq = new PendingQuery(querystring, comicJson)
      Helpers.blockingCall(db.put(pq.toJson(), "pendingqueries"))
      val nj = new NotificationJob(comicJson, 1)
      Helpers.blockingCall(db.put(nj.toJson(), "notificationjobs"))
    }

    "look for a job" should "make pending notifications" in {
      setUpFakeData()
      Helpers.blockingCall(njWorker.lookForJob())
      val pns = db.get("{}", "pendingnotifications")
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

  }

}
