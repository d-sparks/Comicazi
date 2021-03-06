import colossus._
import core._
import service._
import protocols.http._
import UrlParsing._
import HttpMethod._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor._
import endpoints._
import datastore._
import notification.NjActor
import indexes.Mongo

class Comicazi(
  context: ServerContext,
  eps: Endpoints
) extends HttpService(context) {
  def handle = {
    case req @ Post on Root / "comics" => {
      Callback.fromFuture(eps.postComic(req)).map { r => req.ok(r) }
    }
    case req @ Post on Root / "subscriptions" => {
      Callback.fromFuture(eps.postSubscription(req)).map { r => req.ok(r) }
    }
    case req @ get on Root / "notifications" => {
      Callback.fromFuture(eps.getNotifications(req, 50)).map { r => req.ok(r) }
    }
  }
}

class ComicaziInitializer(
  worker: WorkerRef,
  eps: Endpoints
) extends Initializer(worker) {
  def onConnect = context => new Comicazi(context, eps)
}

object Main extends App {
  implicit val sys = ActorSystem()
  implicit val io = IOSystem()

  // Create datastore and build indexes
  val store = new MongoStore("mongodb://172.17.42.1:50001", "comicazi")
  Mongo.build(store.client, "comicazi")

  // Create endpoints
  val eps = new Endpoints(store)

  // Start the server
  Server.start("comicazi", 80) { worker =>
    new ComicaziInitializer(worker, eps)
  }

  // Start the notification actor
  val njWorker = sys.actorOf(Props(new NjActor(store, 1)))
  sys.scheduler.schedule(
    5 seconds,
    5 seconds,
    njWorker,
    "look for a job"
  )

}
