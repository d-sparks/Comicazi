import akka.actor.Actor
import datastore.DataStore

package notification {

  class NjWorker(db: DataStore, n: Int) extends Actor {
    def receive = {
      case "look for a job" => lookForJob()
      case _ => {}
    }

    def lookForJob() = println("looking for a job...")
  }
}

