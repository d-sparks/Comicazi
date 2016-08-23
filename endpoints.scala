import colossus._
import protocols.http._
import scala.concurrent._
import scala.collection._
import ExecutionContext.Implicits.global
import datastore._
import schemas._


package endpoints {

  class Endpoints(datastore: DataStore) {

    def postComic(request: HttpRequest) : Future[String] = {
      // TODO: error handling
      datastore.put(new Comic(request.body.toString()), "comics")
    }

  }

}
