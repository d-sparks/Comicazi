import json.JSON

package schemas {

  object Schemas {
    case class SchemaValue(value: String, req: Boolean)
    type Schema = Map[String, SchemaValue]

    val comicTypes = Map[String, String](
      "publisher" -> "class java.lang.String",
      "year" -> "class java.lang.Integer",
      "mint" -> "class java.lang.Boolean"
    )

    val comic = comicTypes.mapValues({case (fType: String) =>
      SchemaValue(fType, true)
    }).asInstanceOf[Schema]

    val subscription = comicTypes.mapValues({case (fType: String) =>
      SchemaValue(fType, false)
    }).++(Map[String, SchemaValue](
      "email" -> SchemaValue("class java.lang.String", true),
      "querypattern" -> SchemaValue("class java.lang.String", true)
    )).asInstanceOf[Schema]

    // getKeys returns a string of the keys of the given json in alphabetical
    // order.
    def getKeys(json: String) = {
      JSON.toMutableMap(json).keys.toList.sorted.mkString
    }

  }

  // note: determine whether these needs to be a case class
  class Comic(
    private val json: String
  ) extends JsonSchemaEnforcer(json, Schemas.comic)

  class Subscription(
    private val json: String
  ) extends JsonSchemaEnforcer(
    JSON.extend(json, s"""{"querypattern":"${Schemas.getKeys(json)}"}"""),
    Schemas.subscription
  )

  class JsonSchemaEnforcer(
    private val json: String,
    private val schema: Schemas.Schema
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
