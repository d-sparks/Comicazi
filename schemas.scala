import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import scala.collection._

package schemas {

  trait Schema {
    def getData() : Map[String, Any]
    def getJson() : String
  }

  // note: determine whether this needs to be a case class
  class Comic(
    private val json: String
  ) extends JsonSchemaEnforcer(json, true, Map(
      "publisher" -> "class java.lang.String",
      "year" -> "class java.lang.Integer",
      "mint" -> "class java.lang.Boolean"
  )) with Schema

  abstract class JsonSchemaEnforcer(
    private val json: String,
    private val strict: Boolean,
    private val schema: Map[String, String]
  ) {
    private val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    private val d = mapper.readValue[mutable.Map[String, Any]](json)
    if (strict) { schema.foreach({case (k, v) => require(d.contains(k))}) }
    d.foreach({case (k, v) =>
      require(schema.get(k) match {
        case Some(expected) => expected == v.getClass().toString()
        case None => false
      })
    })
    private val enforcedData = d.toMap[String, Any]
    private val enforcedJson = mapper.writeValueAsString(d)
    def getData() = enforcedData
    def getJson() = enforcedJson
  }

}
