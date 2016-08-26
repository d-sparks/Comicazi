import scala.concurrent._
import ExecutionContext.Implicits.global
import org.mongodb.scala.{MongoClient, Observer, Completed, Document}
import scala.util.{Success, Failure}

package datastore {

  trait DataStore {
    def put(doc: String, table: String): Future[String]
    def get(query: String, table: String): Future[String]
    def drop(table: String): Future[Unit]
  }

  class MongoStore(mongoUrl: String, mongoDb: String) extends DataStore {
    val client = MongoClient("mongodb://localhost:27017")
    val db = client.getDatabase(mongoDb)

    def put(doc: String, table: String) : Future[String] = {
      val p = Promise[String]()
      val coll = db.getCollection[Document](table)
      try {
        coll.insertOne(Document(doc)).subscribe(new Observer[Completed] {
          override def onNext(r: Completed): Unit = println(table, "insert", doc)
          override def onError(e: Throwable): Unit = p.failure(e)
          override def onComplete(): Unit = p.success(doc)
        })
        p.future
      }
      catch { case e: Any => { p.failure(e); return p.future } }
    }

    def get(query: String, table: String) : Future[String] = {
      val p = promise[String]
      val coll = db.getCollection[Document](table)
      val bsonQuery = Document(query).toBsonDocument
      val dbResult = coll.find(bsonQuery).toFuture()
      dbResult.onComplete {
        case Success(r) => { p.success(
          if(r.length > 0) r(0).toJson() else ""
        )}
        case Failure(e) => p.failure(e)
      }
      p.future
    }

    def drop(table: String) : Future[Unit] = {
      val p = Promise[Unit]
      val coll = db.getCollection[Document](table)
      coll.drop().subscribe(new Observer[Completed] {
        override def onNext(r: Completed): Unit = println(table, "dropped")
        override def onError(e: Throwable): Unit = p.failure(e)
        override def onComplete(): Unit = p.success()
      })
      p.future
    }

  }

}
