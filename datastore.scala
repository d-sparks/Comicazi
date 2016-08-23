import scala.concurrent._
import ExecutionContext.Implicits.global
import org.mongodb.scala._
import org.mongodb.scala.bson._
import org.mongodb.scala.bson.collection.mutable.Document
import schemas._

package datastore {

  trait DataStore {
    def put(schema: Schema, table: String): Future[String]
    def get(query: String, table: String): Future[String]
  }

  class MongoStore(mongoUrl: String) extends DataStore {
    val client = MongoClient("mongodb://localhost:27017")
    val db = client.getDatabase("comicazi")

    def put(schema: Schema, table: String) : Future[String] = {
      val coll = db.getCollection[Document](table)
      val json = schema.toJson()
      val p = Promise[String]()
      coll.insertOne(Document(json)).subscribe(new Observer[Completed] {
        override def onNext(r: Completed): Unit = println("Inserted comic: " + json)
        override def onError(e: Throwable): Unit = p.failure(e)
        override def onComplete(): Unit = p.success(json)
      })
      p.future
    }

    def get(query: String, table: String) = Future[String] {
      "foo"
    }
  }

}
