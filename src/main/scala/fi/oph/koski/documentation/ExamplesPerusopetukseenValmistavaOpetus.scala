package fi.oph.koski.documentation

import java.time.LocalDate.{of => date}

import fi.oph.koski.documentation.ExampleData._
import fi.oph.koski.documentation.YleissivistavakoulutusExampleData._
import fi.oph.koski.localization.LocalizedString.finnish
import fi.oph.koski.oppija.MockOppijat
import fi.oph.koski.schema._

object ExamplesPerusopetukseenValmistavaOpetus {
  val opiskeluoikeus = PerusopetukseenValmistavanOpetuksenOpiskeluoikeus(
    alkamispäivä = Some(date(2007, 8, 15)),
    päättymispäivä = Some(date(2008, 6, 1)),
    oppilaitos = jyväskylänNormaalikoulu,
    suoritukset = List(
      PerusopetukseenValmistavanOpetuksenSuoritus(
        tila = tilaValmis,
        toimipiste = jyväskylänNormaalikoulu,
        vahvistus = vahvistus(),
        osasuoritukset = Some(List(
          PerusopetukseenValmistavanOpetuksenOppiaineenSuoritus(
            koulutusmoduuli = PerusopetukseenValmistavanOpetuksenOppiaine(
              tunniste = PaikallinenKoodi("ai", finnish("Äidinkieli")),
              laajuus = Some(LaajuusVuosiviikkotunneissa(10)),
              opetuksenSisältö = Some(finnish("Suullinen ilmaisu ja kuullun ymmärtäminen"))
            ),
            tila = tilaValmis,
            arviointi = Some(List(SanallinenPerusopetuksenOppiaineenArviointi(kuvaus = Some(finnish("Keskustelee sujuvasti suomeksi")))))
          )
        ))
      )
    )
  )

  val examples = List(Example("perusopetukseen valmistava opetus", "Oppija on suorittanut perusopetukseen valmistavan opetuksen", Oppija(MockOppijat.koululainen.vainHenkilötiedot, List(opiskeluoikeus))))
}