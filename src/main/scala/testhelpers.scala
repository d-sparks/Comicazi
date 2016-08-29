import scala.concurrent.{Future, Await}
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}
import json.JSON
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

    val ExampleSubscription = new Example(mutable.Map[String,Any](
      "publisher" -> "DC",
      "email" -> "amy@x.ai",
      "querypattern" -> "publisher"
    ))

    class Example(_data: mutable.Map[String,Any]) {
      def asMutableMap() = _data.clone()
      def asMap() = _data.toMap
      def asJson() = JSON.fromMap(asMap())
    }

  }
}
