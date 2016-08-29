import scala.concurrent.{Future, Promise, ExecutionContext}
import scala.util.{Success, Failure}
import ExecutionContext.Implicits.global
import akka.actor.Actor
import datastore.DataStore
import schemas.{NotificationJob, PendingQuery}
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

    def handlePendingQueries(pendingQueries: List[String]) = {
    }

    def performJob(jobs: List[String]) {
      if(jobs.size < 1) { throw new Throwable("No jobs") }
      val job = new NotificationJob(jobs(0))
      val comicB64 = job.toMap.get("comic") match {
        case Some(comic) => comic.asInstanceOf[String]
        case None =>
          throw new Throwable("JsonSchemaEnforcer failure: NotificationJob")
      }
      val pendingQueries = db.get(s"""{"comic":"${comicB64}"}""", "pendingqueries")
      pendingQueries map handlePendingQueries
    }

    def lookForJob() = {
      val getjobs = db.get(s"""{"handler":${n}}""", "notificationjobs")
      getjobs map performJob
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

