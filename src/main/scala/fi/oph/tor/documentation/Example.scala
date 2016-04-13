package fi.oph.tor.documentation

import fi.oph.tor.localization.LocalizedString._
import fi.oph.tor.schema.{Koodistokoodiviite, TorOppija}
case class Example(name: String, description: String, data: TorOppija)

object Examples {
  val examples = ExamplesAmmatillinen.examples ++ ExamplesPeruskoulutus.examples ++ ExamplesLukio.examples
}

object ExampleData {
  lazy val opiskeluoikeusAktiivinen = Koodistokoodiviite("aktiivinen", Some("Aktiivinen"), "opiskeluoikeudentila", Some(1))
  lazy val opiskeluoikeusPäättynyt = Koodistokoodiviite("paattynyt", Some("Päättynyt"), "opiskeluoikeudentila", Some(1))
  lazy val opiskeluoikeusKeskeyttänyt = Koodistokoodiviite("keskeyttanyt", Some("Keskeyttänyt"), "opiskeluoikeudentila", Some(1))
  lazy val suomenKieli = Some(Koodistokoodiviite("FI", Some("suomi"), "kieli", None))
  lazy val tilaKesken = Koodistokoodiviite("KESKEN", "suorituksentila")
  lazy val tilaValmis: Koodistokoodiviite = Koodistokoodiviite(koodistoUri = "suorituksentila", koodiarvo = "VALMIS")
}
