package fi.oph.koski.perftest

import fi.oph.koski.http.Http
import fi.oph.koski.http.Http._

object RandomHetu {
  def nextHetu = hetut.synchronized { hetut.next }

  private lazy val hetut = {
    Iterator.continually({
      println("Haetaan hetuja...")
      runTask(Http("http://www.telepartikkeli.net/tunnusgeneraattori/api")(uri"/generoi/hetu/1000")(Http.parseJson[List[String]])).iterator
    }).flatten
  }
}
