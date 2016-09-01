import helpers.{JSON, Base64}

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

    val querypattern = Map[String, SchemaValue](
      "querypattern" -> SchemaValue("class java.lang.String", true)
    ).asInstanceOf[Schema]

    // Note: affix B64 to field names
    val pendingquery = Map[String, SchemaValue](
      "querystring" -> SchemaValue("class java.lang.String", true),
      "comic" -> SchemaValue("class java.lang.String", true)
    ).asInstanceOf[Schema]

    // Note: affix B64 to "comic" field
    val notificationjob = Map[String, SchemaValue](
      "comic" -> SchemaValue("class java.lang.String", true),
      "handler" -> SchemaValue("class java.lang.Integer", true)
    )

    val pendingnotification = Map[String, SchemaValue](
      "comic" -> SchemaValue("class java.lang.String", true),
      "email" -> SchemaValue("class java.lang.String", true)
    )

  }

  class Comic(
    private val json: String
  ) extends JsonSchemaEnforcer(json, Schemas.comic) {
    def toB64() = Base64.encode(toJson())
    def toB64Qry() = s"""{"comic":"${toB64()}"}"""
  }

  class Subscription(
    private val json: String
  ) extends JsonSchemaEnforcer({
      val filtered = JSON.filter(json, List("_id","email","querypattern"))
      val querypattern = JSON.getKeys(filtered)
      JSON.extend(json, s"""{"querypattern":"${querypattern}"}""")
    },
    Schemas.subscription
  ) {
    val querypattern = JSON.getKeys(
      JSON.filter(toJson(), List("email", "querypattern"))
    )
  }

  class PendingNotification(
    private val json: String
  ) extends JsonSchemaEnforcer(json, Schemas.pendingnotification)

  class QueryPattern(
    private val json: String
  ) extends JsonSchemaEnforcer(json, Schemas.querypattern)

  class PendingQuery(
    private val json: String
  ) extends JsonSchemaEnforcer(json, Schemas.pendingquery) {
    def this(querystring: String, comicJson: String) = this({
      val qs = Base64.encode(querystring)
      val c = Base64.encode(comicJson)
      s"""{"querystring":"${qs}","comic":"${c}"}"""
    })
  }

  class NotificationJob(
    private val json: String
  ) extends JsonSchemaEnforcer(json, Schemas.notificationjob) {
    def this(comicJson: String, handler: Int) = this({
      JSON.fromMap(Map[String, Any](
        "comic" -> Base64.encode(comicJson),
        "handler" -> handler
      ))
    })
  }

  class JsonSchemaEnforcer(
    private val json: String,
    private val schema: Schemas.Schema
  ) {
    private val data = JSON.toMutableMap(json)
    // throw away fields that are not in the schema
    data.retain({case (k, v) => schema.contains(k)})
    // check that all required fields are provided
    for ((k, v) <- schema) if(v.req) { require(data.contains(k)) }
    // enforce type on provided fields, string fields may not contain " marks
    for ((k, v) <- data) {
      require(schema.get(k) match {
        case Some(expected) => expected.value == v.getClass().toString()
        case None => false
      })
    }
    private val enforcedData = data.toMap[String, Any]
    private val enforcedJson = JSON.fromMap(data) // alphabetized
    def toMap() = enforcedData
    def toJson() = enforcedJson
  }

}
