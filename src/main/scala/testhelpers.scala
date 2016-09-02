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
      "artist" -> "mark bagley",
      "author" -> "brian michael bendis",
      "issue" -> 1,
      "mint" -> true,
      "series" -> "ultimate spiderman",
      "superhero" -> "",
      "publisher" -> "marvel",
      "year" -> 2000
    ))

    val ExampleComic2 = new Example(mutable.Map[String,Any](
      "artist" -> "jim lee",
      "author" -> "chris claremont",
      "issue" -> 1,
      "mint" -> true,
      "series" -> "x-men",
      "superhero" -> "wolverine",
      "publisher" -> "marvel",
      "year" -> 1991
    ))

    val ExampleSubscription = new Example(mutable.Map[String,Any](
      "publisher" -> "marvel",
      "series" -> "x-men",
      "issue" -> 1,
      "mint" -> true,
      "email" -> "amy@x.ai",
      "querypattern" -> "publisher,series,superhero"
    ))

    val ExampleSubscription2 = new Example(mutable.Map[String,Any](
      "artist" -> "jim lee",
      "email" -> "amy@x.ai",
      "querypattern" -> "artist"
    ))

    val ExampleSubscription3 = new Example(mutable.Map[String,Any](
      "publisher" -> "dc",
      "series" -> "detective comics",
      "superhero" -> "batman",
      "email" -> "dan.sparks@human.x.ai",
      "querypattern" -> "publisher,series,superhero"
    ))

    val ExampleSubscription4 = new Example(mutable.Map[String, Any](
      "year" -> 1984,
      "email" -> "orwell@dystop.ia",
      "querypattern" -> "year"
    ))

    class Example(_data: mutable.Map[String,Any]) {
      def asMutableMap() = _data.clone()
      def asMap() = _data.toMap
      def asJson() = JSON.fromMap(asMap()).replaceAll("\n", "")
    }

  }
}
