import colossus._
import protocols.http._
import scala.concurrent._
import scala.collection._
import ExecutionContext.Implicits.global
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import datastore._
import schemas._


package endpoints {

  class Endpoints(datastore: DataStore) {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)

    def postComic(request: HttpRequest) : Future[String] = {
      // TODO: error handling
      val bodyDataMap = mapper.readValue[mutable.Map[String, Any]](request.body.toString())
      val comic = new Comic(bodyDataMap)
      datastore.put(comic, "comics")
    }

  }

}
