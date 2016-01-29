package fi.oph.tor.organisaatio

import com.typesafe.config.Config
import fi.oph.tor.config.TorApplication
import fi.oph.tor.json.Json
import fi.oph.tor.koodisto.{KoodistoPalvelu, KoodistoViitePalvelu}

object OrganisaatioMockDataUpdater extends App {
  updateMockDataFromOrganisaatioPalvelu(TorApplication.apply().config)

  def updateMockDataFromOrganisaatioPalvelu(config: Config): Unit = {
    val koodisto = KoodistoViitePalvelu(KoodistoPalvelu.apply(config))
    val organisaatioPalvelu = new RemoteOrganisaatioRepository(config, koodisto)

    MockOrganisaatiot.organisaatiot.foreach(oid => updateMockDataForOrganisaatio(oid, organisaatioPalvelu))
  }

  def updateMockDataForOrganisaatio(oid: String, organisaatioPalvelu: RemoteOrganisaatioRepository): Unit = {
    val tulos = organisaatioPalvelu.fetch(oid)
    Json.writeFile(MockOrganisaatioRepository.filename(oid), tulos)
  }
}