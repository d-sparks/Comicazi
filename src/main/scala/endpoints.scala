import colossus._
import protocols.http._
import scala.concurrent._
// import scala.collection._
import ExecutionContext.Implicits.global
import datastore._
import schemas._

package endpoints {

  class Endpoints(datastore: DataStore) {

    def postComic(request: HttpRequest) : Future[String] = {
      // TODO: error handling
      val body = request.body.toString()
      val comic = new Comic(body)
      datastore.get(comic.toJson(), "comics").flatMap { result =>
        result match {
          case "" => datastore.put(comic.toJson(), "comics")
          case _ => throw new Exception("Comic already exists")
        }
      }
    }

  }

}
