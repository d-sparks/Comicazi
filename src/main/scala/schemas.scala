import json.Converter

package schemas {

  trait Jsonable {
    def toJson() : String
  }

  case class SchemaValue(value: String, req: Boolean)
  case class Schema(m: Map[String, SchemaValue])

  object Schemas {
    val comic = Schema(Map[String, SchemaValue](
      // Required fields
      "publisher" -> SchemaValue("class java.lang.String", true),
      "year" -> SchemaValue("class java.lang.Integer", true),
      "mint" -> SchemaValue("class java.lang.Boolean", true),
      // Optional fields
      "stock" -> SchemaValue("class java.lang.Integer", false)
    ))
  }

  // note: determine whether this needs to be a case class
  class Comic(
    private val json: String
  ) extends JsonSchemaEnforcer(
    json,
    Schemas.comic
  ) with Jsonable

  abstract class JsonSchemaEnforcer(
    private val json: String,
    private val schema: Schema
  ) {
    private val data = Converter.toMutableMap(json)
    // throw away fields that are not in the schema
    data.retain({case (k, v) => schema.m.contains(k)})
    // check that all required fields are provided
    for ((k, v) <- schema.m) if(v.req) require(data.contains(k))
    // enforce type on provided fields
    for ((k, v) <- data) {
      require(schema.m.get(k) match {
        case Some(expected) => expected == v.getClass().toString()
        case None => false
      })
    }
    private val enforcedData = data.toMap[String, Any]
    private val enforcedJson = Converter.fromMap(data)
    def toMap() = enforcedData
    def toJson() = enforcedJson
  }

}
