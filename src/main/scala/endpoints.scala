import colossus._
import protocols.http._
import scala.concurrent._
import scala.collection.mutable.MutableList
import ExecutionContext.Implicits.global
import datastore._
import schemas._

package endpoints {

  class Endpoints(datastore: DataStore) {

    def postComic(request: HttpRequest) : Future[String] = {
      val body = request.body.toString()
      val comic = new Comic(body)
      datastore.put(comic.toJson(), "comics")
    }

    def postSubscription(request: HttpRequest) : Future[String] = {
      val body = request.body.toString()
      val sub = new Subscription(body)
      val qpJson = s"""{"querypattern":"${sub.querypattern}"}"""
      val querypattern = new QueryPattern(qpJson)
      datastore.put(querypattern.toJson(), "querypatterns").flatMap { _ =>
        datastore.put(sub.toJson(), "subscriptions")
      }
    }

  }

}
