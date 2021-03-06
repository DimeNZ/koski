package fi.oph.koski.schema

import java.time.LocalDate

import fi.oph.scalaschema.annotation._

object Opiskeluoikeus {
  type Id = Int
  type Versionumero = Int
  val VERSIO_1 = 1
}

trait Opiskeluoikeus extends OrganisaatioonLiittyvä with Lähdejärjestelmällinen {
  @Description("Opiskeluoikeuden tyyppi, jolla erotellaan eri koulutusmuotoihin (perusopetus, lukio, ammatillinen...) liittyvät opiskeluoikeudet")
  @OksaUri("tmpOKSAID869", "koulutusmuoto (1)")
  @KoodistoUri("opiskeluoikeudentyyppi")
  @Hidden
  def tyyppi: Koodistokoodiviite
  @Description("Opiskeluoikeuden uniikki tunniste, joka generoidaan Koski-järjestelmässä. Tietoja syötettäessä kenttä ei ole pakollinen. " +
    "Tietoja päivitettäessä Koski tunnistaa opiskeluoikeuden joko tämän id:n tai muiden kenttien (oppijaOid, organisaatio, opiskeluoikeuden tyyppi, paikallinen id) perusteella")
  @Hidden
  def id: Option[Int]
  @Description("Versionumero, joka generoidaan Koski-järjestelmässä. Tietoja syötettäessä kenttä ei ole pakollinen. " +
    "Ensimmäinen tallennettu versio saa versionumeron 1, jonka jälkeen jokainen päivitys aiheuttaa versionumeron noston yhdellä. " +
    "Jos tietoja päivitettäessä käytetään versionumeroa, pitää sen täsmätä viimeisimpään tallennettuun versioon. " +
    "Tällä menettelyllä esimerkiksi käyttöliittymässä varmistetaan, ettei tehdä päivityksiä vanhentuneeseen dataan.")
  @Hidden
  def versionumero: Option[Int]
  @Description("Lähdejärjestelmän tunniste ja opiskeluoikeuden tunniste lähdejärjestelmässä. " +
    "Käytetään silloin, kun opiskeluoikeus on tuotu Koskeen tiedonsiirrolla ulkoisesta järjestelmästä, eli käytännössä oppilashallintojärjestelmästä.")
  @Description("Opiskelijan opiskeluoikeuden alkamisaika joko tutkintotavoitteisessa koulutuksessa tai tutkinnon osa tavoitteisessa koulutuksessa. Muoto YYYY-MM-DD")
  def alkamispäivä: Option[LocalDate]
  @Description("Opiskelijan opiskeluoikeuden arvioitu päättymispäivä joko tutkintotavoitteisessa koulutuksessa tai tutkinnon osa tavoitteisessa koulutuksessa. Muoto YYYY-MM-DD")
  def arvioituPäättymispäivä: Option[LocalDate]
  @Description("Opiskelijan opiskeluoikeuden päättymispäivä joko tutkintotavoitteisessa koulutuksessa tai tutkinnon osa tavoitteisessa koulutuksessa. Muoto YYYY-MM-DD")
  def päättymispäivä: Option[LocalDate]
  @Description("Oppilaitos, jossa opinnot on suoritettu")
  def oppilaitos: Oppilaitos
  @Description("Koulutustoimija, käytännössä oppilaitoksen yliorganisaatio")
  @ReadOnly("Tiedon syötössä tietoa ei tarvita; organisaation tiedot haetaan Organisaatiopalvelusta")
  @Hidden
  def koulutustoimija: Option[OidOrganisaatio] // TODO: oid organisaatio on huono kirjoitusasu skemassa
  @Description("Opiskeluoikeuteen liittyvien tutkinto- ja muiden suoritusten tiedot")
  def suoritukset: List[PäätasonSuoritus]
  @Description("Opiskeluoikeuden tila, joka muodostuu opiskeluoikeusjaksoista.")
  def tila: OpiskeluoikeudenTila
  @Description("Läsnä- ja poissaolojaksot päivämääräväleinä.")
  def läsnäolotiedot: Option[Läsnäolotiedot]
  def withKoulutustoimija(koulutustoimija: OidOrganisaatio): Opiskeluoikeus
  def omistajaOrganisaatio = oppilaitos
}

trait KoskeenTallennettavaOpiskeluoikeus extends Opiskeluoikeus {
  def suoritukset: List[PäätasonSuoritus]
  def withIdAndVersion(id: Option[Int], versionumero: Option[Int]): KoskeenTallennettavaOpiskeluoikeus
  def withSuoritukset(suoritukset: List[PäätasonSuoritus]): KoskeenTallennettavaOpiskeluoikeus
  override def läsnäolotiedot: Option[YleisetLäsnäolotiedot]
}

trait OpiskeluoikeudenTila {
  @Representative
  def opiskeluoikeusjaksot: List[Opiskeluoikeusjakso]
}

@Description("Opiskeluoikeuden tilahistoria (aktiivinen, keskeyttänyt, päättynyt...) jaksoittain")
trait Opiskeluoikeusjakso extends Alkupäivällinen {
  @Description("Opiskeluoikeuden tila (aktiivinen, keskeyttänyt, päättynyt...)")
  def tila: Koodistokoodiviite
  def opiskeluoikeusPäättynyt: Boolean
}

trait KoskiOpiskeluoikeusjakso extends Opiskeluoikeusjakso {
  @KoodistoUri("koskiopiskeluoikeudentila")
  def tila: Koodistokoodiviite
  def opiskeluoikeusPäättynyt = List("valmistunut", "eronnut", "katsotaaneronneeksi").contains(tila.koodiarvo)
}

trait Läsnäolotiedot {
  @Description("Läsnä- ja poissaolojaksot päivämääräväleinä.")
  def läsnäolojaksot: List[Läsnäolojakso]
}

case class YleisetLäsnäolotiedot(
  läsnäolojaksot: List[YleinenLäsnäolojakso]
) extends Läsnäolotiedot

trait Alkupäivällinen {
  @Description("Jakson alkamispäivämäärä. Muoto YYYY-MM-DD")
  def alku: LocalDate
}

trait Jakso extends Alkupäivällinen {
  @Description("Jakson loppupäivämäärä. Muoto YYYY-MM-DD")
  def loppu: Option[LocalDate]
}

trait Läsnäolojakso extends Alkupäivällinen {
  @Description("Läsnäolotila (läsnä, poissa...)")
  def tila: Koodistokoodiviite
}

case class YleinenLäsnäolojakso(
  alku: LocalDate,
  @KoodistoUri("lasnaolotila")
  tila: Koodistokoodiviite
) extends Läsnäolojakso

object YleinenLäsnäolojakso {
  def apply(alku: LocalDate, tila: String): YleinenLäsnäolojakso = YleinenLäsnäolojakso(alku, Koodistokoodiviite(tila, "lasnaolotila"))
}
case class LähdejärjestelmäId(
  @Description("Opiskeluoikeuden paikallinen uniikki tunniste lähdejärjestelmässä. Tiedonsiirroissa tarpeellinen, jotta voidaan varmistaa päivitysten osuminen oikeaan opiskeluoikeuteen.")
  id: Option[String],
  @Description("Lähdejärjestelmän yksilöivä tunniste. Tällä tunnistetaan järjestelmä, josta tiedot on tuotu Koskeen. " +
    "Kullakin erillisellä tietojärjestelmäinstanssilla tulisi olla oma tunniste. " +
    "Jos siis oppilaitoksella on oma tietojärjestelmäinstanssi, tulee myös tällä instanssilla olla uniikki tunniste.")
  @KoodistoUri("lahdejarjestelma")
  lähdejärjestelmä: Koodistokoodiviite
)
trait Lähdejärjestelmällinen {
  @Hidden
  def lähdejärjestelmänId: Option[LähdejärjestelmäId]
}