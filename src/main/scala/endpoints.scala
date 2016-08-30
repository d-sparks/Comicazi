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

    // todo: break the flatmaps up into individual functions
    // also: all of this logic should be in the notification worker
    def postComic(request: HttpRequest) : Future[String] = {
      val body = request.body.toString()
      val comic = new Comic(body).toJson()
      // * STEP 1 * find all QueryPattern's
      datastore.get("{}", "querypatterns").flatMap { querypatterns =>
      // * STEP 2 * create relevant PendingQuery's
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
        datastore.putMany(pqsJson.toList, "pendingqueries")
      }.flatMap { _ =>
      // * STEP 3 * create a notification job
        val nj = new NotificationJob(comic, 1)
        datastore.put(nj.toJson, "notificationjobs")
      }.flatMap { _ =>
      // * STEP 4 * add the comic to inventory
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
