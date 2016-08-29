import akka.actor.{ActorSystem, Props}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Span, Seconds, Millis}
import scala.concurrent.{Future, Promise} // ExecutionContext
import scala.collection.mutable.MutableList
import datastore.MongoStore
import notification.NjWorker

package notification {

  class NjWorkerSpec extends FlatSpec with ScalaFutures with Matchers {
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
      njWorker.sequentially[Unit](p, 0, lambdas, List[Boolean]())
      whenReady(p.future) { results =>
        results.length shouldBe 10000
      }
    }

  }

}
