import colossus._
import protocols.http._
import scala.concurrent._
import scala.collection.mutable.MutableList
import ExecutionContext.Implicits.global
import json.{JSON, Base64}
import datastore._
import schemas._

package endpoints {

  class Endpoints(datastore: DataStore) {

    def postComic(request: HttpRequest) : Future[String] = {
      val body = request.body.toString()
      val comic = new Comic(body).toJson()
      datastore.get("{}", "querypatterns").flatMap { querypatterns =>
        val pqs = MutableList[PendingQuery]()
        for (querypattern <- querypatterns) {
          val qp = new QueryPattern(querypattern)
          val fieldsString = qp.toMap().get("querypattern") match {
            case Some(v) => v.asInstanceOf[String]
            case None => throw new Exception("Fire! Fire! Fire!")
          }
          val searchFields = fieldsString.split(",").toList
          val querystring = JSON.project(comic, searchFields)
          pqs += new PendingQuery(querystring, comic)
        }
        val pqsJson = pqs.map({
          (pq: PendingQuery) => pq.toJson()
        })
        if (pqsJson.length > 0) {
          datastore.putMany(pqsJson.toList, "pendingqueries")
        } else {
          datastore.ping()
        }
      }.flatMap { _ =>
        datastore.put(comic, "comics")
      }
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
