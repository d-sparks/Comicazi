import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import sun.misc.{BASE64Encoder, BASE64Decoder}
import scala.collection._

package json {

  object JSON {
    private val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)

    def fromMap(m: Map[String, Any]) = mapper.writeValueAsString(m)
    def toMutableMap(json: String) = {
      mapper.readValue[mutable.Map[String, Any]](json)
    }
    def toMap(json: String) = toMutableMap(json).toMap[String, Any]
    def filter(json: String, fields: List[String]) = {
      val m = toMutableMap(json)
      for (field <- fields) { m.remove(field) }
      fromMap(m.toMap[String,Any])
    }
    def extend(json: String, extension: String) = {
      val m = toMutableMap(json)
      val mExt = toMutableMap(extension)
      for ((k, v) <- mExt) {
        if(!m.contains(k)) m.put(k,v)
      }
      fromMap(m.toMap)
    }
    def getKeys(json: String) = {
      JSON.toMutableMap(json).keys.toList.sorted.mkString(",")
    }
    def project(json: String, fields: List[String]) = {
      fromMap(toMutableMap(json).retain({(k: String, v: Any) =>
        fields.contains(k)
      }).toMap)
    }
  }

  object Base64 {
    private val encoder = new BASE64Encoder()
    private val decoder = new BASE64Decoder()
    def encode(s: String) = encoder.encode(s.toCharArray.map(_.toByte))
    def decode(s: String) = new String(decoder.decodeBuffer(s).map(_.toChar))
  }

}
