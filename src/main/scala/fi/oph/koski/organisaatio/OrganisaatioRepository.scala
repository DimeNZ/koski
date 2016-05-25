package fi.oph.koski.organisaatio

import com.typesafe.config.Config
import fi.oph.koski.cache.{CachingProxy, KoskiCache}
import fi.oph.koski.http.Http._
import fi.oph.koski.http.{Http, VirkailijaHttpClient}
import fi.oph.koski.koodisto.KoodistoViitePalvelu
import fi.oph.koski.localization.LocalizedString
import fi.oph.koski.log.TimedProxy
import fi.oph.koski.schema.OrganisaatioWithOid

trait OrganisaatioRepository {
  /**
   * Organisation hierarchy containing children of requested org. Parents are not included.
   */
  def getOrganisaatioHierarkia(oid: String): Option[OrganisaatioHierarkia] = getOrganisaatioHierarkiaIncludingParents(oid).flatMap(_.find(oid))
  /**
   * Organisation hierarchy containing parents and children of requested org.
   */
  def getOrganisaatioHierarkiaIncludingParents(oid: String): Option[OrganisaatioHierarkia]
  def getOrganisaatio(oid: String): Option[OrganisaatioWithOid] = getOrganisaatioHierarkia(oid).map(_.toOrganisaatio)
  def getChildOids(oid: String): Option[Set[String]] = getOrganisaatioHierarkia(oid).map { hierarkia =>
    def flatten(orgs: List[OrganisaatioHierarkia]): List[OrganisaatioHierarkia] = {
      orgs.flatMap { org => org :: flatten(org.children) }
    }
    flatten(List(hierarkia)).map(_.oid).toSet
  }
  def search(searchTerm: String): List[OrganisaatioWithOid]
}

object OrganisaatioRepository {
  def apply(config: Config, koodisto: KoodistoViitePalvelu) = {
    CachingProxy[OrganisaatioRepository](KoskiCache.cacheStrategy, TimedProxy(withoutCache(config, koodisto).asInstanceOf[OrganisaatioRepository]))
  }

  def withoutCache(config: Config, koodisto: KoodistoViitePalvelu): JsonOrganisaatioRepository = {
    if (config.hasPath("opintopolku.virkailija.url")) {
      val http = VirkailijaHttpClient(config.getString("opintopolku.virkailija.username"), config.getString("opintopolku.virkailija.password"), config.getString("opintopolku.virkailija.url"), "/organisaatio-service")
      new RemoteOrganisaatioRepository(http, koodisto)
    } else {
      new MockOrganisaatioRepository(koodisto)
    }
  }
}

abstract class JsonOrganisaatioRepository(koodisto: KoodistoViitePalvelu) extends OrganisaatioRepository {
  def getOrganisaatioHierarkiaIncludingParents(oid: String): Option[OrganisaatioHierarkia] = {
   fetch(oid).organisaatiot.map(convertOrganisaatio)
      .headOption
  }

  private def convertOrganisaatio(org: OrganisaatioPalveluOrganisaatio): OrganisaatioHierarkia = {
    val oppilaitosnumero = org.oppilaitosKoodi.flatMap(oppilaitosnumero => koodisto.getKoodistoKoodiViite("oppilaitosnumero", oppilaitosnumero))
    OrganisaatioHierarkia(org.oid, oppilaitosnumero, LocalizedString.sanitizeRequired(org.nimi, org.oid), org.organisaatiotyypit, org.children.map(convertOrganisaatio))
  }

  def search(searchTerm: String): List[OrganisaatioWithOid] = fetchSearch(searchTerm).organisaatiot.map(convertOrganisaatio).map(_.toOrganisaatio)

  def fetch(oid: String): OrganisaatioHakuTulos
  def fetchSearch(searchTerm: String): OrganisaatioHakuTulos
}

class RemoteOrganisaatioRepository(http: Http, koodisto: KoodistoViitePalvelu) extends JsonOrganisaatioRepository(koodisto) {
  def fetch(oid: String): OrganisaatioHakuTulos = {
    runTask(http(uri"/organisaatio-service/rest/organisaatio/v2/hierarkia/hae?aktiiviset=true&lakkautetut=false&oid=${oid}")(Http.parseJson[OrganisaatioHakuTulos]))
  }
  def fetchSearch(searchTerm: String): OrganisaatioHakuTulos = {
    runTask(http(uri"/organisaatio-service/rest/organisaatio/v2/hae?aktiiviset=true&lakkautetut=false&searchStr=${searchTerm}")(Http.parseJson[OrganisaatioHakuTulos]))
  }
}

case class OrganisaatioHakuTulos(organisaatiot: List[OrganisaatioPalveluOrganisaatio])
case class OrganisaatioPalveluOrganisaatio(oid: String, nimi: Map[String, String], oppilaitosKoodi: Option[String], organisaatiotyypit: List[String], children: List[OrganisaatioPalveluOrganisaatio])
