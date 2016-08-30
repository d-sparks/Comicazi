import scala.concurrent.{Future, Promise, ExecutionContext}
import scala.util.{Success, Failure}
import ExecutionContext.Implicits.global
import akka.actor.Actor
import datastore.DataStore
import schemas.{
  NotificationJob,
  PendingQuery,
  PendingNotification,
  Subscription,
  Comic
}
import json.{Base64, JSON}

package notification {

  class NjWorker(db: DataStore, n: Int) {

    def thr(msg: String) = new Throwable(msg)
    def jseThr(culprit: String) = thr("JsonSchemaEnforcer fail: " + culprit)

    def sequentially[T](
      p: Promise[List[Boolean]],
      i: Int,
      tasks: List[() => Future[T]],
      results: List[Boolean]
    ) : Unit = {
      if(tasks.length == 0) {
        return p.success(List[Boolean]())
      }
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

    // def makeNotifyTask(pn: String) = {
    //   () => {

    //   }
    // }

    // def notifyNAtATime(p: Promise[Unit], comic: Comic, n: Int) = {

    // }

    def beginNotifying(comic: Comic)(x: Unit) = {
      // Want to do this: find first 100, do something, repeat
      // temp:
      val p = Promise[Unit]
      p.success()
      p.future
    }

    def checkPqsExhaustive(comic: Comic)(x: Unit) = {
      val p = Promise[Unit]
      db.get(comic.toB64Qry, "pendingqueries") map { results =>
        if(results.length == 0) {
          p.success("success")
        } else {
          p.failure(thr("PendingQuery remaining"))
        }
      }
      p.future flatMap beginNotifying(comic)
    }

    def checkPqsSuccess(comic: Comic)(pqSuccesses: List[Boolean]) = {
      val p = Promise[Unit]
      if(pqSuccesses.contains(false)) {
        p.failure(thr("A query pattern failed"))
        p.future
      } else {
        p.success()
        p.future flatMap checkPqsExhaustive(comic)
      }
    }

    def matchesHandler(
      pq: PendingQuery,
      comic: Comic
    )(matches: List[String]) = {
      val pns = matches.map { case(subJson: String) =>
        val subscription = new Subscription(subJson)
        val email = subscription.toMap.get("email") match {
          case Some(subEmail) => subEmail.asInstanceOf[String]
          case None => throw jseThr("Subscription")
        }
        val pnJson = JSON.extend(comic.toB64Qry, s"""{"email":"${email}"}""")
        new PendingNotification(pnJson).toJson()
      }.toList
      db.putMany(pns, "pendingnotifications") flatMap { _ =>
        db.remove(pq.toJson, "pendingqueries")
      }
    }

    def handlePendingQueries(comic: Comic)(pqs: List[String]) = {
      val queryHandlers = pqs.map { case(pqJson: String) =>
        {() => {
          val pq = new PendingQuery(pqJson)
          val qsB64 = pq.toMap.get("querystring") match {
            case Some(str) => str.asInstanceOf[String]
            case None => throw jseThr("PendingQuery")
          }
          val qs = Base64.decode(qsB64)
          db.get(qs, "subscriptions") flatMap matchesHandler(pq, comic)
        }}
      }
      val p = Promise[List[Boolean]]
      sequentially(p, 0, queryHandlers, List[Boolean]())
      p.future flatMap checkPqsSuccess(comic)
    }

    def performJob(jobs: List[String]) = {
      if(jobs.size < 1) { throw thr("No jobs") }
      val job = new NotificationJob(jobs(0))
      val comicB64 = job.toMap.get("comic") match {
        case Some(comic) => comic.asInstanceOf[String]
        case None => throw jseThr("NotificationJob")
      }
      val comic = new Comic(Base64.decode(comicB64))
      val pqs = db.get(comic.toB64Qry, "pendingqueries")
      pqs flatMap handlePendingQueries(comic)
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

