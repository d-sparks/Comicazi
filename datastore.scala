import scala.util._
import scala.collection._
import scala.concurrent._
import ExecutionContext.Implicits.global
import org.mongodb.scala._
import org.mongodb.scala.bson._
import org.mongodb.scala.bson.collection.mutable.Document
import scala.reflect.runtime.universe._
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
      // Create insertable document
      var bsonDoc = new BsonDocument()
      schema.getData().foreach {
        case(k, v) => v match {
          case _: String => bsonDoc.append(k, BsonString(v.asInstanceOf[String]))
          case _: Number => bsonDoc.append(k, BsonInt32(v.asInstanceOf[Int]))
          case _: Boolean => bsonDoc.append(k, BsonBoolean(v.asInstanceOf[Boolean]))
        }
      }
      val doc = Document(bsonDoc)

      // Insert document the Mongo way
      val coll = db.getCollection[Document](table)
      val observable = coll.insertOne(doc)

      val out = Promise[String]()
      val successString = "success"
      observable.subscribe(new Observer[Completed] {
        override def onNext(result: Completed): Unit = println("Inserted comic")
        override def onError(e: Throwable): Unit = out.failure(e)
        override def onComplete(): Unit = out.success(successString)
      })
      out.future
    }

    def get(query: String, table: String) = Future[String] {
      "foo"
    }
  }

}
