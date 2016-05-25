package fi.oph.koski.schema

import fi.oph.koski.localization.LocalizedString._
import fi.oph.koski.localization.{Localizable, LocalizedString}
import fi.oph.scalaschema.annotation.{MinValue, Description}

trait Koulutusmoduuli extends Localizable {
  def tunniste: KoodiViite
  def laajuus: Option[Laajuus]
  def nimi: LocalizedString
  def description: LocalizedString = nimi
  def isTutkinto = false
}

trait KoodistostaLöytyväKoulutusmoduuli extends Koulutusmoduuli {
  def tunniste: Koodistokoodiviite
  def nimi: LocalizedString = tunniste.nimi.getOrElse(unlocalized(tunniste.koodiarvo))
}

trait EPerusteistaLöytyväKoulutusmoduuli extends Koulutusmoduuli {
  @Description("Tutkinnon perusteen diaarinumero Ks. ePerusteet-palvelu")
  def perusteenDiaarinumero: Option[String]
}

trait PaikallinenKoulutusmoduuli extends Koulutusmoduuli {
  def tunniste: Paikallinenkoodi
  def nimi = tunniste.nimi
}

@Description("Tutkinnon tai tutkinnon osan laajuus. Koostuu opintojen laajuuden arvosta ja yksiköstä")
trait Laajuus {
  @Description("Opintojen laajuuden arvo")
  @MinValue(0)
  def arvo: Float
  @Description("Opintojen laajuuden yksikkö")
  @KoodistoUri("opintojenlaajuusyksikko")
  def yksikkö: Koodistokoodiviite
}