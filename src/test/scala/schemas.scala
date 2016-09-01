import org.scalatest.{FlatSpec, Matchers}
import schemas.Comic
import scala.util.control.Breaks.{break, breakable}
import helpers.JSON
import testhelpers.Helpers.ExampleComic
import schemas._
import schemas.Defs.{Schema, SchemaValue}
import testhelpers.Helpers

package schemas {

  class SchemaSpec extends FlatSpec with Matchers

  trait JsonSchemaBehaviors { this: SchemaSpec =>
    def jsonschemaenforcer(
      json: String,
      schema: Schema
    ) {
      it should "filter fields not in the schema" in {
        val bogusJson = JSON.extend(json, """{"bogus_field":true}""")
        val instance = (new JsonSchemaEnforcer(json, schema)).toMap()
        val keys = instance.keys
        for (k <- keys) { instance should contain key k }
      }

      it should "fail if required fields aren't included" in {
        val instance = JSON.toMutableMap(json)
        val reqFields = schema.filter({
          case (k: String, x: SchemaValue) => x.req
        }).keys
        breakable { for (k <- reqFields) { if(instance.contains(k)) {
          instance.remove(k)
          break
        }}}
        val bogusJson = JSON.fromMap(instance.toMap)
        try {
          new JsonSchemaEnforcer(bogusJson, schema)
          throw new Exception()
        }
        catch { case e: Throwable =>
          e shouldBe a [java.lang.IllegalArgumentException]
        }
      }
    }
  }

  class ComicSpec extends SchemaSpec with JsonSchemaBehaviors {
    val comicJson = Helpers.ExampleComic.asJson()
    val comicSchema = Defs.comic
    it should behave like jsonschemaenforcer(comicJson, comicSchema)
  }

  class SubscriptionSpec extends SchemaSpec with JsonSchemaBehaviors {
    val subJson = Helpers.ExampleSubscription3.asJson()
    val subSchema = Defs.subscription
    it should behave like jsonschemaenforcer(subJson, subSchema)
  }

}
