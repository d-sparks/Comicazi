import colossus._
import protocols.http._
import scala.concurrent._
import scala.collection.mutable.MutableList
import ExecutionContext.Implicits.global
import json.{JSON, Base64}
import datastore.DataStore
import schemas._

package endpoints {

  class Endpoints(db: DataStore) {

    def postComic(request: HttpRequest) : Future[String] = {
      val body = request.body.toString()
      val comicJson = new Comic(body).toJson()
      db.get("{}", "querypatterns") flatMap insertQueryPatterns(comicJson)
    }

    def insertQueryPatterns(comicJson: String)(qpsJson: List[String]) = {
      // Turn QueryPatterns (qp) into PendingQueries (pq)
      val pqsJson = qpsJson.map {case(querypattern: String) =>
        val qp = new QueryPattern(querypattern)
        val fieldsString = qp.toMap().get("querypattern") match {
          case Some(v) => v.asInstanceOf[String]
          case None => throw new Exception("JsonSchemaEnforcer fail: PendingQuery")
        }
        val searchFields = fieldsString.split(",").toList
        val querystring = JSON.project(comicJson, searchFields)
        new PendingQuery(querystring, comicJson).toJson
      }.toList
      db.putMany(pqsJson, "pendingqueries") flatMap createJob(comicJson)
    }

    def createJob(comicJson: String)(putResult: String) = {
      val nj = new NotificationJob(comicJson, 1)
      db.put(nj.toJson, "notificationjobs") flatMap insertComic(comicJson)
    }

    def insertComic(comicJson: String)(putResult: String) = {
      db.put(comicJson, "comics")
    }

    def postSubscription(request: HttpRequest) : Future[String] = {
      val body = request.body.toString()
      val sub = new Subscription(body)
      val qpJson = s"""{"querypattern":"${sub.querypattern}"}"""
      val querypattern = new QueryPattern(qpJson)
      db.put(querypattern.toJson(), "querypatterns").flatMap { _ =>
        db.put(sub.toJson(), "subscriptions")
      }
    }

  }

}
