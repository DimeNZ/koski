package fi.oph.tor.api

import java.time.LocalDate
import fi.oph.tor.json.Json._
import fi.oph.tor.organisaatio.MockOrganisaatiot
import fi.oph.tor.schema._
import org.json4s.JValue

trait OpiskeluOikeusData {
  val defaultHenkilö = NewHenkilö("010101-123N", "Testi", "Testi", "Toivola")

  def makeOppija(henkilö: Henkilö = defaultHenkilö, opiskeluOikeudet: List[AnyRef] = List(opiskeluoikeus())): JValue = toJValue(Map(
    "henkilö" -> henkilö,
    "opiskeluoikeudet" -> opiskeluOikeudet
  ))

  val autoalanPerustutkinto: TutkintoKoulutus = TutkintoKoulutus(KoodistoKoodiViite("351301", "koulutus"), Some("39/011/2014"))

  val tutkintototeutus: TutkintoKoulutustoteutus = TutkintoKoulutustoteutus(autoalanPerustutkinto)

  def tutkintoSuoritus(toteutus: TutkintoKoulutustoteutus = tutkintototeutus, tila: Option[KoodistoKoodiViite] = None): Suoritus = Suoritus(None, toteutus, None, None, None, toimipiste = OidOrganisaatio(MockOrganisaatiot.lehtikuusentienToimipiste), None, None, None)

  def opiskeluoikeus(toteutus: TutkintoKoulutustoteutus = tutkintototeutus) = OpiskeluOikeus(None, None, None, None, None, None,
    oppilaitos = Oppilaitos(MockOrganisaatiot.stadinAmmattiopisto),
    suoritus = tutkintoSuoritus(toteutus), None, None, None, None
  )

  val laajuus = Laajuus(11, KoodistoKoodiViite("6", "opintojenlaajuusyksikko"))

  val tutkinnonOsa: OpsTutkinnonosa = OpsTutkinnonosa(KoodistoKoodiViite("100023", "tutkinnonosat"), true, Some(laajuus), None, None)

  val tutkinnonOsaToteutus: OpsTutkinnonosatoteutus = OpsTutkinnonosatoteutus(tutkinnonOsa, None, None)

  def arviointiHyvä(päivä: Option[LocalDate] = None): Some[List[Arviointi]] = Some(List(Arviointi(KoodistoKoodiViite("2", "arviointiasteikkoammatillinent1k3"), päivä)))

  val tilaValmis: Some[KoodistoKoodiViite] = Some(KoodistoKoodiViite("VALMIS", "suorituksentila"))
  val tilaKesken: Some[KoodistoKoodiViite] = Some(KoodistoKoodiViite("KESKEN", "suorituksentila"))
  val tilaKeskytynyt: Some[KoodistoKoodiViite] = Some(KoodistoKoodiViite("KESKEYTYNYT", "suorituksentila"))

  val tutkinnonOsaSuoritus = Suoritus(
    None, tutkinnonOsaToteutus, None, tilaKesken, None,
    OidOrganisaatio("1.2.246.562.10.42456023292", Some("Stadin ammattiopisto, Lehtikuusentien toimipaikka")),
    arviointiHyvä(), None, None)
}
