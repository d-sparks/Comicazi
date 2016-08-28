import scala.concurrent.{Future, Promise, ExecutionContext}
import ExecutionContext.Implicits.global
import scala.collection.mutable.MutableList
import scala.collection.mutable
import org.mongodb.scala.{
  MongoClient, Observer, Completed, Document, BulkWriteResult
}
import org.mongodb.scala.model.{InsertOneModel, WriteModel}
import scala.util.{Success, Failure}
import json.JSON

package datastore {

  trait DataStore {
    def put(doc: String, table: String): Future[String]
    def putMany(docs: List[String], table: String): Future[String]
    def get(query: String, table: String): Future[List[String]]
    def drop(table: String): Future[Unit]
  }

  // MongoStore uses MongoDB as a DataStore

  class MongoStore(mongoUrl: String, mongoDb: String) extends DataStore {
    val client = MongoClient("mongodb://localhost:27017")
    val db = client.getDatabase(mongoDb)
    def put(doc: String, table: String) : Future[String] = {
      val p = Promise[String]()
      val coll = db.getCollection[Document](table)
      try {
        coll.insertOne(Document(doc)).subscribe(new Observer[Completed] {
          override def onNext(r: Completed): Unit = { /* Logging */ }
          override def onError(e: Throwable): Unit = p.failure(e)
          override def onComplete(): Unit = p.success(doc)
        })
        p.future
      }
      catch {
        case e: Any => { p.failure(e); p.future }
      }
    }
    def putMany(docs: List[String], table: String) : Future[String] = {
      val p = Promise[String]()
      val coll = db.getCollection[Document](table)
      try {
        val ops = docs.map({(doc: String) =>
          InsertOneModel(Document(doc)).asInstanceOf[WriteModel[Document]]
        })
        coll.bulkWrite(ops).subscribe(new Observer[BulkWriteResult] {
          override def onNext(r: BulkWriteResult): Unit = { println(r) }
          override def onError(e: Throwable): Unit = p.failure(e)
          override def onComplete(): Unit = p.success("done")
        })
        p.future
      }
      catch {
        case e: Any => { p.failure(e); p.future }
      }
    }
    def get(query: String, table: String) : Future[List[String]] = {
      val p = Promise[List[String]]
      val coll = db.getCollection[Document](table)
      val bsonQuery = Document(query).toBsonDocument
      val dbResult = coll.find(bsonQuery).toFuture()
      dbResult.onComplete {
        case Success(r) => p.success(
          r.map({(res: Document) =>
            JSON.filter(res.toJson(), List("_id"))
          }).toList
        )
        case Failure(e) => p.failure(e)
      }
      p.future
    }
    def drop(table: String) : Future[Unit] = {
      val p = Promise[Unit]
      val coll = db.getCollection[Document](table)
      coll.drop().subscribe(new Observer[Completed] {
        override def onNext(r: Completed): Unit = { /* Logging */ }
        override def onError(e: Throwable): Unit = p.failure(e)
        override def onComplete(): Unit = p.success()
      })
      p.future
    }
  }

}
