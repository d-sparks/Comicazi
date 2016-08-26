import org.scalatest.{FlatSpec, Matchers}
import schemas.Comic
import json.Converter

package schemas {
  class ComicSpec extends FlatSpec with Matchers {
    "Comic constructor" should "filter fields not in the schema" in {
      val comicJson = Converter.fromMap(Map[String,Any](
        "publisher" -> "DC",
        "year" -> 1973,
        "mint" -> true,
        "bogus" -> "should be filtered"
      ))
      val comic = new Comic(comicJson)
      comic.toJson shouldBe Converter.filterFields(comicJson, List("bogus"))
    }

    it should "fail if required fields aren't provided" in {
      val comicJson = Converter.fromMap(Map[String,Any](
        "publisher" -> "DC",
        "year" -> 1973
      ))
      try { new Comic(comicJson) }
      catch {
        case e: Throwable => e shouldBe a [java.lang.IllegalArgumentException]
      }
    }
  }
}
