import colossus._
import core._
import service._
import protocols.http._
import UrlParsing._
import HttpMethod._
import akka.actor._
import endpoints._
import datastore._

class Comicazi(
  context: ServerContext,
  eps: Endpoints
) extends HttpService(context) {
  def handle = {
    case req @ Post on Root / "comics" => {
      Callback.fromFuture(eps.postComic(req)).map { r => req.ok(r) }
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

  val eps = new Endpoints(new MongoStore("localhost:27017", "comicazi"))

  Server.start("comicazi", 9000) {
    worker => new ComicaziInitializer(worker, eps)
  }
}
