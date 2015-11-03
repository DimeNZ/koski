package fi.oph.tor.organisaatio

import com.typesafe.config.Config
import fi.oph.tor.http.{Http, VirkailijaHttpClient}

trait OrganisaatioRepository {
  def getOrganisaatio(oid: String): Option[Organisaatio]
}

class RemoteOrganisaatioRepository(config: Config) extends OrganisaatioRepository{
  val virkailijaClient = new VirkailijaHttpClient(config.getString("authentication-service.username"), config.getString("authentication-service.password"), config.getString("opintopolku.virkailija.url"), "/organisaatio-service")

  def getOrganisaatio(oid: String): Option[Organisaatio] = {
    virkailijaClient.httpClient(virkailijaClient.virkailijaUriFromString("/organisaatio-service/rest/organisaatio/v2/hierarkia/hae/tyyppi?aktiiviset=true&lakkautetut=false&oid=" + oid))(Http.parseJson[OrganisaatioHakuTulos])
      .organisaatiot.map(convertOrganisaatio)
      .headOption
  }

  private def convertOrganisaatio(org: OrganisaatioPalveluOrganisaatio): Organisaatio = {
    Organisaatio(org.oid, org.nimi("fi"), org.organisaatiotyypit, org.children.map(convertOrganisaatio))
  }
}

case class OrganisaatioHakuTulos(organisaatiot: List[OrganisaatioPalveluOrganisaatio])
case class OrganisaatioPalveluOrganisaatio(oid: String, nimi: Map[String, String], organisaatiotyypit: List[String], children: List[OrganisaatioPalveluOrganisaatio])