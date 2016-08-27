import scala.concurrent.{Future, Await}
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}
import json.Converter
import schemas.{Comic}

package testhelpers {
  object Helpers {

    def blockingCall[T](f: Future[T]) = {
      Await.ready(f, Duration.Inf).value.get match {
        case Failure(e) => throw e
        case Success(result) => Right(result)
      }
    }

    object ExampleComic {
      private val _data = mutable.Map[String,Any](
        "publisher" -> "DC",
        "year" -> 1973,
        "mint" -> true
      )
      def asMutableMap() = _data.clone()
      def asMap() = _data.toMap
      def asJson() = Converter.fromMap(asMap())
      def asComic() = new Comic(asJson())
    }

  }
}
