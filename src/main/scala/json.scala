import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import scala.collection._

package json {

  object Converter {
    private val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    def fromMap(m: Map[String, Any]) = mapper.writeValueAsString(m)
    def toMutableMap(json: String) = {
      mapper.readValue[mutable.Map[String, Any]](json)
    }
    def toMap(json: String) = toMutableMap(json).toMap[String, Any]
  }

}