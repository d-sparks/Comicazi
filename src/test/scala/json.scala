import org.scalatest._
import json.{JSON, Base64}

package json {
  class JSONSpec extends FlatSpec with Matchers {
    "project" should "project onto the given fields" in {
      val json = """{"a":"b","c":"d"}"""
      val fields = List("a", "e")
      JSON.project(json, fields) shouldBe """{"a":"b"}"""
    }

    it should "return an empty json object if all fields filtered" in {
      val json = """{"a":"b","c":"d"}"""
      val fields = List()
      JSON.project(json, fields) shouldBe """{}"""
    }
  }

  class BSONSpec extends FlatSpec with Matchers {
    "decode(encode(s)" should "be s" in {
      val s = "s9df8asd89fhjaklsj#(&H@D<KK%THK"
      Base64.decode(Base64.encode(s)) shouldBe s
    }
  }
}
