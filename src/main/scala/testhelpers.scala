import scala.concurrent.{Future, Await}
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}
import helpers.JSON
import schemas.{Comic}

package testhelpers {
  object Helpers {

    def blockingCall[T](f: Future[T]) = {
      Await.ready(f, Duration.Inf).value.get match {
        case Failure(e) => throw e
        case Success(result) => Right(result)
      }
    }

    val ExampleComic = new Example(mutable.Map[String,Any](
      "publisher" -> "DC",
      "year" -> 1973,
      "mint" -> true
    ))

    val ExampleComic2 = new Example(mutable.Map[String,Any](
      "publisher" -> "Marvel",
      "year" -> 1986,
      "mint" -> false
    ))

    val ExampleSubscription = new Example(mutable.Map[String,Any](
      "publisher" -> "DC",
      "email" -> "amy@x.ai",
      "querypattern" -> "publisher"
    ))

    val ExampleSubscription2 = new Example(mutable.Map[String,Any](
      "mint" -> true,
      "email" -> "amy@x.ai",
      "querypattern" -> "mint"
    ))

    val ExampleSubscription3 = new Example(mutable.Map[String,Any](
      "publisher" -> "Marvel",
      "year" -> 1986,
      "mint" -> false,
      "email" -> "dan.sparks@humans.x.ai",
      "querypattern" -> "publisher,year,mint"
    ))

    val ExampleSubscription4 = new Example(mutable.Map[String, Any](
      "year" -> 1984,
      "email" -> "orwell@dystop.ia",
      "querypattern" -> "year"
    ))

    class Example(_data: mutable.Map[String,Any]) {
      def asMutableMap() = _data.clone()
      def asMap() = _data.toMap
      def asJson() = JSON.fromMap(asMap())
    }

  }
}
