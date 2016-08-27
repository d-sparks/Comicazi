import org.scalatest.{FlatSpec, Matchers}
import schemas.Comic
import scala.util.control.Breaks.{break, breakable}
import json.JSON
import testhelpers.Helpers.ExampleComic
import schemas._
import schemas.Schemas.{Schema, SchemaValue}
import testhelpers.Helpers

package schemas {
  trait JsonSchemaBehaviors { this: FlatSpec with Matchers =>
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
          throw new Exception("Wrong exception type")
        }
        catch { case e: Throwable =>
          e shouldBe a [java.lang.IllegalArgumentException]
        }
      }
    }
  }

  class ComicSpec extends FlatSpec with Matchers with JsonSchemaBehaviors {
    val comicJson = Helpers.ExampleComic.asJson()
    val comicSchema = Schemas.comic
    it should behave like jsonschemaenforcer(comicJson, comicSchema)
  }

}
