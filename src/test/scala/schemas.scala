import org.scalatest.{FlatSpec, Matchers}
import schemas.Comic
import json.JSON
import testhelpers.Helpers.ExampleComic

package schemas {
  class ComicSpec extends FlatSpec with Matchers {
    "Comic constructor" should "filter fields not in the schema" in {
      val example = ExampleComic.asMutableMap()
      // add a field to be filtered
      example.put("bogus", "should be filtered")
      // construct a comic
      val comicMap = example.toMap
      val comicJson = JSON.fromMap(comicMap)
      val comic = new Comic(comicJson)
      // verify the field is filtered in the comic object
      val expectedComicToJson = JSON.filterFields(comicJson, List("bogus"))
      comic.toJson shouldBe expectedComicToJson
    }

    it should "fail if required fields aren't provided" in {
      val example = ExampleComic.asMutableMap()
      // remove a required field
      example.remove("mint")
      // make json for comic constructor
      val comicJson = JSON.fromMap(Map[String,Any](
        "publisher" -> "DC",
        "year" -> 1973
      ))
      // make sure this throws an exception
      try { new Comic(comicJson); throw new Exception("Wrong") }
      catch {
        case e: Throwable => e shouldBe a [java.lang.IllegalArgumentException]
      }
    }
  }
}
