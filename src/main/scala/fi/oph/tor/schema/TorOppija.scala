package fi.oph.tor.schema

import java.time.LocalDate
import fi.oph.tor.schema.generic.annotation.{Description, ReadOnly}


case class TorOppija(
  henkilö: Henkilö,
  @Description("Lista henkilön opiskeluoikeuksista. Sisältää vain ne opiskeluoikeudet, joihin käyttäjällä on oikeudet. Esimerkiksi ammatilliselle toimijalle ei välttämättä näy henkilön lukio-opintojen tietoja")
  opiskeluOikeudet: Seq[OpiskeluOikeus]
)

@Description("Henkilötiedot. Syötettäessä vaaditaan joko `oid` tai kaikki muut kentät, jolloin järjestelmään voidaan tarvittaessa luoda uusi henkilö")
sealed trait Henkilö {}

@Description("Täydet henkilötiedot. Tietoja haettaessa TOR:sta saadaan aina täydet henkilötiedot.")
case class FullHenkilö(
  @Description("Yksilöivä tunniste Opintopolku-palvelussa")
  @OksaUri("tmpOKSAID760", "oppijanumero")
  oid: String,
  @Description("Suomalainen henkilötunnus")
  hetu: String,
  @Description("Henkilön kaikki etunimet. Esimerkiksi Sanna Katariina")
  etunimet:String,
  @Description("Kutsumanimi, oltava yksi etunimistä. Esimerkiksi etunimille \"Juha-Matti Petteri\" kelpaavat joko \"Juha-Matti\", \"Juha\", \"Matti\" tai \"Petteri\"")
  kutsumanimi: String,
  sukunimi: String
) extends Henkilö

@Description("Henkilö, jonka oid ei ole tiedossa. Tietoja syötettäessä luodaan mahdollisesti uusi henkilö Henkilöpalveluun")
case class NewHenkilö(
  @Description("Suomalainen henkilötunnus")
  hetu: String,
  @Description("Henkilön kaikki etunimet. Esimerkiksi Sanna Katariina")
  etunimet:String,
  @Description("Kutsumanimi, oltava yksi etunimistä. Esimerkiksi etunimille \"Juha-Matti Petteri\" kelpaavat joko \"Juha-Matti\", \"Juha\", \"Matti\" tai \"Petteri\"")
  kutsumanimi: String,
  sukunimi: String
) extends Henkilö

@Description("Henkilö, jonka oid on tiedossa. Tietoja syötettäessä henkilö haetaan henkilöpalvelusta.")
case class OidHenkilö(
  @Description("Yksilöivä tunniste Opintopolku-palvelussa")
  @OksaUri("tmpOKSAID760", "oppijanumero")
  oid: String
) extends Henkilö

object Henkilö {
  type Id = String
  def withOid(oid: String) = OidHenkilö(oid)
  def apply(hetu: String, etunimet: String, kutsumanimi: String, sukunimi: String) = NewHenkilö(hetu, etunimet, kutsumanimi, sukunimi)
}

case class OpiskeluOikeus(
  @Description("Opiskeluoikeuden uniikki tunniste. Tietoja syötettäessä kenttä ei ole pakollinen. Tietoja päivitettäessä TOR tunnistaa opiskeluoikeuden joko tämän id:n tai muiden kenttien (oppijaOid, organisaatio, diaarinumero) perusteella")
  id: Option[Int],
  @Description("Paikallinen tunniste opiskeluoikeudelle. Tiedonsiirroissa tarpeellinen, jotta voidaan varmistaa päivitysten osuminen oikeaan opiskeluoikeuteen.")
  paikallinenId: Option[String],
  @Description("Opiskelijan opiskeluoikeuden alkamisaika joko tutkintotavoitteisessa koulutuksessa tai tutkinnon osa tavoitteisessa koulutuksessa. Muoto YYYY-MM-DD")
  alkamispäivä: Option[LocalDate],
  @Description("Opiskelijan opiskeluoikeuden arvioitu päättymispäivä joko tutkintotavoitteisessa koulutuksessa tai tutkinnon osa tavoitteisessa koulutuksessa. Muoto YYYY-MM-DD")
  arvioituPäättymispäivä: Option[LocalDate],
  @Description("Opiskelijan opiskeluoikeuden päättymispäivä joko tutkintotavoitteisessa koulutuksessa tai tutkinnon osa tavoitteisessa koulutuksessa. Muoto YYYY-MM-DD")
  päättymispäivä: Option[LocalDate],
  @Description("Oppilaitos, jossa opinnot on suoritettu")
  oppilaitos: Organisaatio,
  @Description("Opiskeluoikeuteen liittyvän (tutkinto-)suorituksen tiedot")
  suoritus: Suoritus,
  hojks: Option[Hojks],
  @Description("Opiskelijan suorituksen tavoite-tieto kertoo sen, suorittaako opiskelija tutkintotavoitteista koulutusta (koko tutkintoa) vai tutkinnon osa tavoitteista koulutusta (tutkinnon osaa)")
  @KoodistoUri("opintojentavoite")
  tavoite: Option[KoodistoKoodiViite],
  läsnäolotiedot: Option[Läsnäolotiedot],
  @Description("Opintojen rahoitus")
  @KoodistoUri("opintojenrahoitus")
  opintojenRahoitus: Option[KoodistoKoodiViite]
)

case class Suoritus(
  @Description("Paikallinen tunniste suoritukselle. Tiedonsiirroissa tarpeellinen, jotta voidaan varmistaa päivitysten osuminen oikeaan suoritukseen.")
  paikallinenId: Option[String],
  @Description("Koulutusmoduulin tunniste. Joko tutkinto tai tutkinnon osa")
  koulutusmoduulitoteutus: Koulutusmoduulitoteutus,
  @Description("Opintojen suorituskieli")
  @KoodistoUri("kieli")
  @OksaUri("tmpOKSAID309", "opintosuorituksen kieli")
  suorituskieli: Option[KoodistoKoodiViite],
  @Description("Suorituksen tila")
  @KoodistoUri("suorituksentila")
  tila: Option[KoodistoKoodiViite],
  alkamispäivä: Option[LocalDate],
  @Description("Oppilaitoksen toimipiste, jossa opinnot on suoritettu")
  @OksaUri("tmpOKSAID148", "koulutusorganisaation toimipiste")
  toimipiste: Organisaatio,
  arviointi: Option[List[Arviointi]],
  vahvistus: Option[Vahvistus],
  osasuoritukset: Option[List[Suoritus]]
)

trait Koulutusmoduuli
  @Description("Tutkintoon johtava koulutus")
  case class TutkintoKoulutus(
    @Description("Tutkinnon 6-numeroinen tutkintokoodi")
    @KoodistoUri("koulutus")
    @OksaUri("tmpOKSAID560", "tutkinto")
    tutkintokoodi: KoodistoKoodiViite,
    @Description("Tutkinnon perusteen diaarinumero (pakollinen). Ks. ePerusteet-palvelu")
    perusteenDiaarinumero: Option[String]
  ) extends Koulutusmoduuli

  @Description("Opetussuunnitelmaan kuuluva tutkinnon osa")
  case class OpsTutkinnonosa(
    @Description("Tutkinnon osan kansallinen koodi")
    @KoodistoUri("tutkinnonosat")
    tutkinnonosakoodi: KoodistoKoodiViite,
    @Description("Onko pakollinen osa tutkinnossa")
    pakollinen: Boolean,
    paikallinenKoodi: Option[Paikallinenkoodi] = None,
    kuvaus: Option[String] = None
  ) extends Koulutusmoduuli

  @Description("Paikallinen tutkinnon osa")
  case class PaikallinenTutkinnonosa(
    paikallinenKoodi: Paikallinenkoodi,
    kuvaus: String,
    @Description("Onko pakollinen osa tutkinnossa")
    pakollinen: Boolean
  ) extends Koulutusmoduuli

trait Koulutusmoduulitoteutus
  @Description("Tutkintoon johtava koulutus")
  case class TutkintoKoulutustoteutus(
    koulutusmoduuli: TutkintoKoulutus,
    @Description("Tieto siitä mihin tutkintonimikkeeseen oppijan tutkinto liittyy")
    @KoodistoUri("tutkintonimikkeet")
    @OksaUri("tmpOKSAID588", "tutkintonimike")
    tutkintonimike: Option[List[KoodistoKoodiViite]] = None,
    @Description("Osaamisala")
    @KoodistoUri("osaamisala")
    @OksaUri(tunnus = "tmpOKSAID299", käsite = "osaamisala")
    osaamisala: Option[List[KoodistoKoodiViite]] = None,
    @Description("Tutkinnon tai tutkinnon osan suoritustapa")
    @OksaUri("tmpOKSAID141", "ammatillisen koulutuksen järjestämistapa")
    suoritustapa: Option[Suoritustapa],
    @Description("Koulutuksen järjestämismuoto")
    @OksaUri("tmpOKSAID140", "koulutuksen järjestämismuoto")
    järjestämismuoto: Option[Järjestämismuoto]
  ) extends Koulutusmoduulitoteutus

  @Description("Opetussuunnitelmaan kuuluva tutkinnon osa")
  case class OpsTutkinnonosatoteutus(
    koulutusmoduuli: OpsTutkinnonosa,
    @Description("Tutkinnon tai tutkinnon osan suoritustapa")
    @OksaUri("tmpOKSAID141", "ammatillisen koulutuksen järjestämistapa")
    suoritustapa: Option[Suoritustapa] = None,
    hyväksiluku: Option[Hyväksiluku] = None
  ) extends Koulutusmoduulitoteutus

  @Description("Paikallinen tutkinnon osa")
  case class PaikallinenTutkinnonosatoteutus(
    koulutusmoduuli: PaikallinenTutkinnonosa,
    @Description("Tutkinnon tai tutkinnon osan suoritustapa")
    @OksaUri("tmpOKSAID141", "ammatillisen koulutuksen järjestämistapa")
    suoritustapa: Option[Suoritustapa],
    hyväksiluku: Option[Hyväksiluku] = None
  ) extends Koulutusmoduulitoteutus

case class Arviointi(
  @Description("Arvosana. Kullekin arviointiasteikolle löytyy oma koodistonsa")
  arvosana: KoodistoKoodiViite,
  päivä: Option[LocalDate],
  @Description("Onko kyseessä arvosanan korotus")
  arvosananKorottaminen: Option[Boolean],
  @Description("Tutkinnon osan suorituksen arvioinnista päättäneen henkilön nimi")
  arvioitsijat: Option[List[Arvioitsija]]
)

case class Arvioitsija(
  nimi: String
)

case class Vahvistus(
  @Description("Tutkinnon tai tutkinnonosan vahvistettu suorituspäivämäärä, eli päivämäärä jolloin suoritus on hyväksyttyä todennettua osaamista.")
  päivä: Option[LocalDate]
)

trait Suoritustapa {
  def tunniste: KoodistoKoodiViite
}

@Description("Suoritustapa ilman lisätietoja")
case class DefaultSuoritustapa(
  @KoodistoUri("suoritustapa")
  tunniste: KoodistoKoodiViite
) extends Suoritustapa

@Description("Suoritustapa näyttötietojen kanssa")
case class NäytöllinenSuoritustapa(
  @KoodistoUri("suoritustapa")
  //@KoodistoArvo("naytto")
  tunniste: KoodistoKoodiViite,
  näyttö: Näyttö
) extends Suoritustapa

trait Järjestämismuoto {
  def tunniste: KoodistoKoodiViite
}

@Description("Järjestämismuoto ilman lisätietoja")
case class DefaultJärjestämismuoto(
  @KoodistoUri("jarjestamismuoto")
  tunniste: KoodistoKoodiViite
) extends Järjestämismuoto

case class OppisopimuksellinenJärjestämismuoto(
  @KoodistoUri("jarjestamismuoto")
  tunniste: KoodistoKoodiViite,
  oppisopimus: Oppisopimus
) extends Järjestämismuoto

case class Hyväksiluku(
  @Description("Aiemman, korvaavan suorituksen kuvaus")
  osaaminen: Koulutusmoduuli,
  @Description("Osaamisen tunnustamisen kautta saatavan tutkinnon osan suorituksen selite")
  @OksaUri("tmpOKSAID629", "osaamisen tunnustaminen")
  selite: Option[String]
)

@Description("Näytön kuvaus")
case class Näyttö(
  kuvaus: String,
  suorituspaikka: String
)

@Description("Oppisopimuksen tiedot")
case class Oppisopimus(
  työnantaja: Yritys
)

case class Yritys(
  nimi: String,
  yTunnus: String
)

case class Läsnäolotiedot(
  läsnäolojaksot: List[Läsnäolojakso]
)

case class Läsnäolojakso(
  alku: LocalDate,
  loppu: Option[LocalDate],
  @Description("Läsnäolotila (läsnä, poissa...)")
  @KoodistoUri("lasnaolotila")
  tila: KoodistoKoodiViite
)

case class Kunta(koodi: String, nimi: Option[String])

case class KoodistoKoodiViite(
  @Description("Koodin tunniste koodistossa")
  koodiarvo: String,
  @Description("Koodin selväkielinen, kielistetty nimi")
  @ReadOnly("Tiedon syötössä kuvausta ei tarvita; kuvaus haetaan Koodistopalvelusta")
  nimi: Option[String],
  @Description("Käytetyn koodiston tunniste")
  koodistoUri: String,
  @Description("Käytetyn koodiston versio. Jos versiota ei määritellä, käytetään uusinta versiota")
  koodistoVersio: Option[Int]
)

@Description("Henkilökohtainen opetuksen järjestämistä koskeva suunnitelma, https://fi.wikipedia.org/wiki/HOJKS")
@OksaUri("tmpOKSAID228", "erityisopiskelija")
case class Hojks(
  hojksTehty: Boolean,
  @KoodistoUri("opetusryhma")
  opetusryhmä: Option[KoodistoKoodiViite]
)

@Description("Paikallinen, koulutustoimijan oma kooditus koulutukselle. Käytetään kansallisen koodiston puuttuessa")
case class Paikallinenkoodi(
  @Description("Koodin tunniste koodistossa")
  koodiarvo: String,
  @Description("Koodin selväkielinen nimi")
  nimi: String,
  @Description("Koodiston tunniste")
  koodistoUri: String
)

case class Organisaatio(
  @Description("Organisaation tunniste Opintopolku-palvelussa")
  oid: String,
  @Description("Organisaation (kielistetty) nimi")
  @ReadOnly("Tiedon syötössä nimeä ei tarvita; kuvaus haetaan Organisaatiopalvelusta")
  nimi: Option[String] = None
)