import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

package testhelpers {
  object Helpers {
    def blockingCall[T](f: Future[T]) = {
      Await.ready(f, Duration.Inf).value.get match {
        case Failure(e) => throw e
        case Success(result) => Right(result)
      }
    }
  }
}
