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
import helpers.{Base64, JSON, Async}

package notification {

  // NjActor is the akka Actor that waits for signals and kicks off a job when
  // it receives a signal. It basically makes an NjWorker into an Actor.

  class NjActor(db: DataStore, n: Int) extends Actor {
    val worker = new NjWorker(db, n)
    def receive = {
      case "look for a job" => worker.lookForJob()
      case _ => {}
    }
  }

  // NjWorker is the class that actually generates notifications. It is
  // tolerant to interrupts at any step, but could potentially be a long
  // running job in the case of tens of millions of subscriptions.

  class NjWorker(db: DataStore, n: Int) {

    // Syntactic sugar for Throwables.
    def thr(msg: String) = new Throwable(msg)
    def jseThr(culprit: String) = thr("JsonSchemaEnforcer fail: " + culprit)

    // Look and see if there is a job waiting to be handled.
    def lookForJob() = {
      val getjobs = db.get(s"""{"handler":${n}}""", "notificationjobs")
      getjobs flatMap performJob
    }

    // Perform a job. Turn the encoded comic into a comic, and find all
    // PendingQueries (these are created along with the NotificationJob by the
    // postComic endpoint).
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

    // Sequentially but in a non-thread-blocking fashion, handle each
    // PendingQuery. This means performing the query, and for any mataches,
    // calling a handler for those matches.
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
      Async.sequentially(p, 0, queryHandlers, List[Boolean]())
      p.future flatMap checkPqsSuccess(comic)
    }

    // For a given PendingQuery, matchesHandler takes the results of that query
    // and creates PendingNotification's for each. If all of the
    // PendingNotification's are created successfully, this handler deletes the
    // PendingQuery to indicate that it is finished. As noted in the index spec
    // in indexes.scala, there is a unique key on the pendingnotifications
    // table, so that even if a subscriber matches multiple queries, their email
    // will only be entered once.
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

    // After handlePendingQueries, we check that all pending queries succeeded.
    // If they did not, we fail, and begin again.
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

    // To be sure it's time to move on to actually creating notifications, we
    // separately check that all PendingQuery's for this comic have been
    // removed.
    def checkPqsExhaustive(comic: Comic)(x: Unit) = {
      val p = Promise[Unit]
      db.get(comic.toB64Qry, "pendingqueries") map { results =>
        if(results.length == 0) {
          p.success()
        } else {
          p.failure(thr("PendingQuery remaining"))
        }
      }
      p.future flatMap notify(comic)
    }

    // It's time to actually notify!  We notify 50 at a time, an arbitrarily
    // chosen number.
    def notify(comic: Comic)(x: Unit) = {
      val p = Promise[Unit]
      notifyNAtATime(p, comic, 50)
      p.future flatMap {_ =>
        db.remove(comic.toB64Qry, "notificationjobs")
      }
    }

    // This is a tail recursive non-blocking (to whatever extent that is true
    // of Futures) function that grabs the first N PendingNotifications,
    // and notifies those subscribers. Because this is recursive, there is a
    // possibility that if things get into a bad state, this might recurse
    // forever. In a production environment, we'd likely track when jobs begin
    // and have the empty subscription subscribed with a monitoring email. If
    // we don't receive an email some amount of time after a NotificationJob
    // began, we'd bring the humans in to see what's up.
    def notifyNAtATime(p: Promise[Unit], comic: Comic, n: Int) : Unit = {
      val f = db.getN(n, comic.toB64Qry, "pendingnotifications")
      f.onComplete {
        case Success(results) => {
          if(results.length == 0) {
            p.success()
          } else {
            val q = Promise[List[Boolean]]
            val notifyTasks = results.map(makeNotifyTask(comic))
            Async.sequentially(q, 0, notifyTasks, List[Boolean]())
            q.future map { _ => notifyNAtATime(p, comic, n)}
          }
        }
        case Failure(_) => p.failure(thr("Database failure"))
      }
    }

    // Given a comic and PendingNotification, notifies the user, and places a
    // Notification into the notifications table. This function is called
    // sequentially N times at a time, by each recursion of notifyNAtATime.
    def makeNotifyTask(comic: Comic)(pnJson: String) = {
      () => {
        // Send an email, but as a proxy, we place a notification into the
        // notifications collection, and so that there's some output, we'll
        // print the notification to the console:
        val p = Promise[String]
        val pn = new PendingNotification(pnJson)
        val email = pn.toMap.get("email") match {
          case Some(str) => str.asInstanceOf[String]
          case None => throw jseThr("PendingNotification")
        }
        // These don't print when the program is being run even though the code
        // is executing, should investigate why. They do print from the tests,
        // however.
        println("----------------------------------")
        println(s"Notification for: ${email}")
        println("A new comic you may be interested in:")
        println(comic.toJson)
        println("----------------------------------")
        // Here we would actually send the email, and flatmap it to this, thus
        // only putting the notification into our historical records if the
        // email was successful.
        db.put(pn.toJson, "notifications") flatMap { _ =>
          db.remove(pn.toJson, "pendingnotifications")
        }
      }
    }

  }

}
