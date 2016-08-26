import scala.concurrent._
import ExecutionContext.Implicits.global
import org.mongodb.scala._
import org.mongodb.scala.bson._
import scala.util.{Success, Failure}
import org.mongodb.scala.bson.collection.mutable.Document

package datastore {

  trait DataStore {
    def put(doc: String, table: String): Future[String]
    def get(query: String, table: String): Future[String]
  }

  class MongoStore(mongoUrl: String, mongoDb: String) extends DataStore {
    val client = MongoClient("mongodb://localhost:27017")
    val db = client.getDatabase(mongoDb)

    def put(doc: String, table: String) : Future[String] = {
      val p = Promise[String]()
      val coll = db.getCollection[Document](table)
      coll.insertOne(Document(doc)).subscribe(new Observer[Completed] {
        override def onNext(r: Completed): Unit = println("Comic insert: " + doc)
        override def onError(e: Throwable): Unit = p.failure(e)
        override def onComplete(): Unit = p.success(doc)
      })
      p.future
    }

    def get(query: String, table: String) : Future[String] = {
      val p = promise[String]
      val coll = db.getCollection[String](table)
      val bsonQuery = Document(query).toBsonDocument
      val dbResult = coll.find(bsonQuery).toFuture()
      dbResult.onComplete {
        case Success(results) => { p.success(
          if(results.length > 0) results(0).toJson() else ""
        )}
        case Failure(e) => p.failure(e)
      }
      p.future
    }

  }

}
