import scala.collection._

package schemas {

  trait Schema {
    def getData() : Map[String, Any]
  }

  abstract class SchemaEnforcer(data: mutable.Map[String, Any], schema: Map[String, String]) {
    schema.foreach({case (k, v) => require(data.contains(k))})
    data.foreach({case (k, v) =>
      require(schema.get(k) match {
        case Some(expected) => expected == v.getClass().toString()
        case None => false
      })
    })
    val enforcedData = data.toMap[String, Any]
    def getData() = enforcedData
  }

  // note: determine whether this needs to be a case class
  class Comic(data: mutable.Map[String, Any]) extends SchemaEnforcer(data, Map(
    "publisher" -> "class java.lang.String",
    "year" -> "class java.lang.Integer",
    "mint" -> "class java.lang.Boolean"
  )) with Schema

}
