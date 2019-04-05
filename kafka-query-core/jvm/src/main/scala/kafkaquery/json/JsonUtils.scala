package kafkaquery.json
import io.circe.Json

object JsonUtils {

  type Path     = List[String]
  type JsonPath = String

  /**
    * flattens the json paths for all the entries
    *
    * e.g.
    *
    * {{{
    *   {
    *     "foo" : { "bar" : [ { "id" :  1 } ] }
    *     "bazz" : true
    *   }
    * }}}
    *
    * would 'flatten' to:
    *
    * {{{
    *   "foo.bar.0.id" -> 1 // where '1' is a JsonInt
    *   "bazz" -> true
    * }}}
    *
    *
    * @param json the json to flatten
    * @return a collection of json paths and their values
    */
  def flatten(json: Json): List[(JsonPath, Json)] = {
    flattenList(json).map {
      case (revPath, value) => revPath.mkString(".") -> value
    }
  }

  def flattenList(json: Json): List[(Path, Json)] = {
    flattenRecursive(json, Nil, Nil).map {
      case (revPath, value) => revPath.reverse -> value
    }
  }

  private def flattenRecursive(json: Json, reversePath: Path, results: List[(Path, Json)]): List[(Path, Json)] = {

    val arrayOpt: Option[List[(Path, Json)]] = json.asArray.map { parts =>
      parts.zipWithIndex.foldLeft(results) {
        case (r, (json, i)) => flattenRecursive(json, i.toString :: reversePath, r)
      }
    }

    def objOpt: Option[List[(Path, Json)]] = json.asObject.map { jsonObj =>
      jsonObj.toIterable.foldLeft(results) {
        case (r, (key, json)) => flattenRecursive(json, key :: reversePath, r)
      }
    }

    def simpleEntry: List[(Path, Json)] = (reversePath -> json) :: results

    arrayOpt.orElse(objOpt).getOrElse(simpleEntry)
  }
}
