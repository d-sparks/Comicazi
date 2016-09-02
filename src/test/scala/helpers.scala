import org.scalatest._
import helpers.{JSON, Base64}
import scala.concurrent.{Future, Promise} // ExecutionContext
import org.scalatest.time.{Span, Seconds, Millis}
import org.scalatest.concurrent.ScalaFutures

package helpers {
  class JSONSpec extends FlatSpec with Matchers {
    "project" should "project onto the given fields" in {
      val json = """{"a":"b","c":"d"}"""
      val fields = List("a", "e")
      JSON.project(json, fields) shouldBe """{"a":"b"}"""
    }

    it should "return an empty json object if all fields filtered" in {
      val json = """{"a":"b","c":"d"}"""
      val fields = List()
      JSON.project(json, fields) shouldBe """{}"""
    }
  }

  class BSONSpec extends FlatSpec with Matchers {
    "decode(encode(s)" should "be s" in {
      val s = "s9df8asd89fhjaklsj#(&H@D<KK%THK"
      Base64.decode(Base64.encode(s)) shouldBe s
    }
  }

  class AsyncSpec extends FlatSpec with ScalaFutures with Matchers {
    implicit val defaultPatience = PatienceConfig(
      timeout = Span(10, Seconds),
      interval = Span(50, Millis)
    )

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
      Async.sequentially[Unit](p, 0, lambdas, List[Boolean]())
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
      Async.sequentially(p, 0, List(f1, f2), List[Boolean]())
      whenReady(p.future) { results =>
        results shouldBe List(false, true)
      }
    }

    it should "work for large tasks lists" in {
      val lambdas = (1 to 10000).map({case(_) =>
        () => {val p = Promise[Unit]; p.success(); p.future}
      }).toList
      val p = Promise[List[Boolean]]
      Async.sequentially(p, 0, lambdas, List[Boolean]())
      whenReady(p.future) { results =>
        results.length shouldBe 10000
      }
    }

  }
}
