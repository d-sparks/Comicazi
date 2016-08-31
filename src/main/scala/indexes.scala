import scala.concurrent.{Future, Promise, ExecutionContext}
import ExecutionContext.Implicits.global
import org.mongodb.scala.{MongoClient, Observer}
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import helpers.{BSON, Async}

package indexes {
  case class Index(_index: String, _options: String)

  object Mongo {

    val unique = new IndexOptions()
    unique.unique(true)
    unique.background(true)
    val background = new IndexOptions()
    background.background(true)

    // This is the index specification. The function that builds them is below.

    val indexSpec = Map[String, Seq[IndexModel]](

      // The strategy here is based on the hypothesis that query patterns (a
      // comma separate list of keys in the subscription (e.g,
      // "publisher,year") will be fairly well varied as the subscriptions
      // table grows large. There are 2^n possible query patterns, where n is
      // the number of fields on the comic book schema. The ideal case would
      // be uniform distribution, so if we had, say, 10 comic book fields and
      // ten million subscribers, we could narrow to 10^7/2^10 ≈ 10,000
      // subscribers. Then, Mongo (during its index strategy assessment) would
      // choose one of these indices, so that far fewer than 10,000 rows would
      // be scanned in a given "PendingQuery". Of course, uniform distribution
      // is unlikely, so for ten million users or more, we'd probably want an
      // intelligent system that determines popular or varied fields and
      // possibly background builds compound indexes on (querypattern,f1,f2)
      // where f1 and f2 are fields which occur together frequently and are
      // varied, or something along those lines.

      "subscriptions" -> Seq(
        IndexModel(BSON.fromMap(Map[String, Any](
          "querypattern" -> 1
        )), background),
        IndexModel(BSON.fromMap(Map[String, Any](
          "querypattern" -> 1,
          "publisher" -> 1
        )), background),
        IndexModel(BSON.fromMap(Map[String, Any](
          "querypattern" -> 1,
          "year" -> 1
        )), background),
        IndexModel(BSON.fromMap(Map[String, Any](
          "querypattern" -> 1,
          "mint" -> 1
        )), background)
      ),

      // We only allow one job at a time for a given comic (hence the
      // unique), but generally want to look them up by handler.

      "notificationjobs" -> Seq(
        IndexModel(BSON.fromMap(Map[String, Any](
          "comic" -> 1
        )), unique),
        IndexModel(BSON.fromMap(Map[String, Any](
          "handler" -> 1
        )), background)
      ),

      // A NjWorker queries for PendingQuery's on the comic field.

      "pendingqueries" -> Seq(
        IndexModel(BSON.fromMap(Map[String, Any](
          "comic" -> 1
        )), background)
      ),

      // These are uniquely indexed because a given user may have many
      // subscriptions that a given comic matches, and even though we
      // safegaurd from notifying the same person about the same comic twice,
      // it reduces workload to let the database reject duplicat writes in the
      // first place. The comic field is first in this compound index because
      // that is what NjWorker searches on.

      "pendingnotifications" -> Seq(
        IndexModel(BSON.fromMap(Map[String, Any](
          "comic" -> 1,
          "email" -> 1
        )), unique)
      ),

      // Similar to pendingnotifications, they are unique to ensure we dont
      // notify a user twice about a given comic. However, these would likely
      // be looked up by email.

      "notifications" -> Seq(
        IndexModel(BSON.fromMap(Map[String, Any](
          "email" -> 1,
          "comic" -> 1
        )), unique)
      )

    )

    def build(mongoClient: MongoClient, dbName: String) : Future[Unit] = {
      val db = mongoClient.getDatabase(dbName)

      // Create a list of tasks, of type () => Future[Unit], to be performed
      // with the Async.sequentially method.

      val indexTasks = indexSpec.map {
        case(table: String, indexes: Seq[IndexModel]) => {() => {
          val p = Promise[Unit]
          val coll = db.getCollection(table)
          coll.createIndexes(
            indexes.asInstanceOf[Seq[IndexModel]]
          ).subscribe(new Observer[String] {
            override def onNext(r: String): Unit = { /* Logging */ }
            override def onError(e: Throwable): Unit = p.failure(e)
            override def onComplete(): Unit = p.success()
          })
          p.future
        }}
      }.toList

      val p = Promise[List[Boolean]]
      Async.sequentially(p, 0, indexTasks, List[Boolean]())
      p.future map { _ => }
    }
  }
}
