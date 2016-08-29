import scala.concurrent.{Future, Promise, ExecutionContext}
import scala.util.{Success, Failure}
import ExecutionContext.Implicits.global
import akka.actor.Actor
import datastore.DataStore
import schemas.{
  NotificationJob,
  PendingQuery,
  PendingNotification,
  Subscription
}
import json.Base64

package notification {

  class NjWorker(db: DataStore, n: Int) {
    def sequentially[T](
      p: Promise[List[Boolean]],
      i: Int,
      tasks: List[() => Future[T]],
      results: List[Boolean]
    ) : Unit = {
      def nextOrSucceed(result: Boolean) = {
        if(i == tasks.length - 1) {
          p.success(results ++ List(result))
        } else {
          sequentially[T](p, i + 1, tasks, results ++ List(result))
        }
      }
      val nearFuture = tasks(i)()
      nearFuture.onComplete {
        case Success(_) => nextOrSucceed(true)
        case Failure(_) => nextOrSucceed(false)
      }
    }

    def beginNotifying(comic: String)(pqSuccesses: List[Boolean]) = {
      val p = Promise[Unit]
      // Temporary:
      p.success()
      p.future
    }

    def matchesHandler(qs: String, comic: String)(matches: List[String]) = {
      val pns = matches.map { case(subJson: String) =>
        val subscription = new Subscription(subJson)
        val email = subscription.toMap.get("email") match {
          case Some(subEmail) => subEmail
          case None => "invalidemail"
        }
        val comicB64 = Base64.encode(comic)
        val pnJson = s"""{"email":"${email}","comic":"${comicB64}"}"""
        new PendingNotification(pnJson).toJson
      }.toList
      db.putMany(pns, "pendingnotifications")
    }

    def handlePendingQueries(comic: String)(pendingQueries: List[String]) = {
      val queryHandlers = pendingQueries.map { case(pqJson: String) =>
        {() => {
          val pqMap = new PendingQuery(pqJson).toMap()
          val qsB64 = pqMap.get("querystring") match {
            case Some(qs) => qs.asInstanceOf[String]
            case None => "definitely not a false positive"
          }
          val qs = Base64.decode(qsB64)
          db.get(qs, "subscriptions") flatMap matchesHandler(qs, comic)
        }}
      }
      val p = Promise[List[Boolean]]
      sequentially(p, 0, queryHandlers, List[Boolean]())
      p.future flatMap beginNotifying(comic)
    }

    def performJob(jobs: List[String]) = {
      if(jobs.size < 1) { throw new Throwable("No jobs") }
      val job = new NotificationJob(jobs(0))
      val comicB64 = job.toMap.get("comic") match {
        case Some(comic) => comic.asInstanceOf[String]
        case None =>
          throw new Throwable("JsonSchemaEnforcer failure: NotificationJob")
      }
      val pendingQueries = db.get(s"""{"comic":"${comicB64}"}""", "pendingqueries")
      pendingQueries flatMap handlePendingQueries(Base64.decode(comicB64))
    }

    def lookForJob() = {
      val getjobs = db.get(s"""{"handler":${n}}""", "notificationjobs")
      getjobs flatMap performJob
    }

  }

  class NjActor(db: DataStore, n: Int) extends Actor {
    val worker = new NjWorker(db, n)
    def receive = {
      case "look for a job" => worker.lookForJob()
      case _ => {}
    }
  }

}

