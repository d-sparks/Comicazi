import colossus._
import protocols.http._
import scala.concurrent._
import scala.collection.mutable.MutableList
import ExecutionContext.Implicits.global
import helpers.{JSON, Base64}
import datastore.DataStore
import schemas._

package endpoints {

  class Endpoints(db: DataStore) {

    // postComic: creates the data necessary for a notification job, which are
    // the QueryPattern's and the NotificationJob, and then inserts the comic
    // into inventory. Note that while there are unique indexes on the
    // notification job data, puts will succeed, so the comic will only not be
    // put into inventory if there is a real database error. But this also
    // ensures that we have a notification job scheduled before updating
    // inventory, so as not to fail to send notifications.

    def postComic(request: HttpRequest) : Future[String] = {
      val body = request.body.toString()
      val comicJson = new Comic(body).toJson()
      db.get("{}", "querypatterns") flatMap insertQueryPatterns(comicJson)
    }

    def insertQueryPatterns(comicJson: String)(qpsJson: List[String]) = {
      // Turn QueryPatterns (qp) into PendingQueries (pq)
      val pqsJson = qpsJson.map {case(qpJson: String) =>
        val qp = new QueryPattern(qpJson)
        val querypattern = qp.toMap().get("querypattern") match {
          case Some(v) => v.asInstanceOf[String]
          case None => throw new Exception("JsonSchemaEnforcer fail: PendingQuery")
        }
        val searchFields = querypattern.split(",").toList
        val querystringMinusPattern = JSON.project(comicJson, searchFields)
        val querystring = JSON.extend(querystringMinusPattern, qpJson)
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
