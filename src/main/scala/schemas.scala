import json.JSON

package schemas {

  object Schemas {
    case class SchemaValue(value: String, req: Boolean)
    type Json = Map[String, SchemaValue]

    val comic = Map[String, SchemaValue](
      // Required fields
      "publisher" -> SchemaValue("class java.lang.String", true),
      "year" -> SchemaValue("class java.lang.Integer", true),
      "mint" -> SchemaValue("class java.lang.Boolean", true)
    ).asInstanceOf[Json]
  }

  // note: determine whether this needs to be a case class
  class Comic(
    private val json: String
  ) extends JsonSchemaEnforcer(
    json,
    Schemas.comic
  )

  abstract class JsonSchemaEnforcer(
    private val json: String,
    private val schema: Schemas.Json
  ) {
    private val data = JSON.toMutableMap(json)
    // throw away fields that are not in the schema
    data.retain({case (k, v) => schema.contains(k)})
    // check that all required fields are provided
    for ((k, v) <- schema) if(v.req) require(data.contains(k))
    // enforce type on provided fields
    for ((k, v) <- data) {
      require(schema.get(k) match {
        case Some(expected) => expected.value == v.getClass().toString()
        case None => false
      })
    }
    private val enforcedData = data.toMap[String, Any]
    private val enforcedJson = JSON.fromMap(data)
    def toMap() = enforcedData
    def toJson() = enforcedJson
  }

}
