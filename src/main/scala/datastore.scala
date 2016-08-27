import scala.concurrent.{Future, Promise, ExecutionContext}
import ExecutionContext.Implicits.global
import scala.collection.mutable.MutableList
import scala.collection.mutable
import org.mongodb.scala.{MongoClient, Observer, Completed, Document}
import scala.util.{Success, Failure}
import json.Converter

package datastore {

  trait DataStore {
    def put(doc: String, table: String): Future[String]
    def get(query: String, table: String): Future[String]
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
      catch { case e: Any => { p.failure(e); return p.future } }
    }
    def get(query: String, table: String) : Future[String] = {
      val p = Promise[String]
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
        override def onNext(r: Completed): Unit = { /* Logging */ }
        override def onError(e: Throwable): Unit = p.failure(e)
        override def onComplete(): Unit = p.success()
      })
      p.future
    }
  }

  // InMemoryStore stores in memory as a Map of strings to lists

  class InMemoryStore extends DataStore {
    val data = mutable.Map[String,MutableList[String]]()
    def put(doc: String, table: String): Future[String] = {
      val p = Promise[String]
      try {
        val jsonDoc = Converter.fromMap(Converter.toMap(doc))
        data.getOrElseUpdate(table, MutableList[String]()) += jsonDoc
        val p = Promise[String]
        p.success(jsonDoc)
        p.future
      }
      catch { case e: Any => { p.failure(e); return p.future } }
    }
    def get(query: String, table: String): Future[String] = {
      val p = Promise[String]
      val in = data.getOrElseUpdate(table, MutableList[String]()).contains(query)
      p.success(if(in) query else "")
      p.future
    }
    def drop(table: String) = Future { data.put(table, MutableList[String]()) }
  }

}
