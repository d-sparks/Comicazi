import org.scalatest._
import json.JSON

package json {
  class ProjectSpec extends FlatSpec with Matchers {
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
}
