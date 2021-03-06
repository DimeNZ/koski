package fi.oph.koski.api

import fi.oph.koski.http.KoskiErrorCategory
import fi.oph.koski.json.Json
import fi.oph.koski.localization.LocalizedString
import fi.oph.koski.oppija.MockOppijat
import fi.oph.koski.schema._
import fi.oph.koski.koskiuser.MockUsers
import java.time.LocalDate.{of => date}

import fi.oph.koski.documentation.AmmatillinenExampleData
import AmmatillinenExampleData._
import fi.oph.koski.organisaatio.MockOrganisaatiot._
import fi.oph.koski.organisaatio.{MockOrganisaatiot, Opetushallitus}
import org.json4s._
import org.scalatest.FunSpec

class OppijaValidationSpec extends FunSpec with LocalJettyHttpSpecification with OpiskeluoikeusTestMethodsAmmatillinen {
  describe("Opiskeluoikeuden lisääminen") {
    describe("Valideilla tiedoilla") {
      it("palautetaan HTTP 200") {
        putOpiskeluOikeus(defaultOpiskeluoikeus) {
          verifyResponseStatus(200)
        }
      }
    }

    describe("Ilman tunnistautumista") {
      it("palautetaan HTTP 401") {
        putOpiskeluOikeus(defaultOpiskeluoikeus, headers = jsonContent) {
          verifyResponseStatus(401, KoskiErrorCategory.unauthorized("Käyttäjä ei ole tunnistautunut."))
        }
      }
    }

    describe("Epäkelpo JSON-dokumentti") {
      it("palautetaan HTTP 400 virhe" ) (request("api/oppija", "application/json", "not json", "put")
        (verifyResponseStatus(400, KoskiErrorCategory.badRequest.format.json("Epäkelpo JSON-dokumentti"))))
    }

    describe("Väärä Content-Type") {
      it("palautetaan HTTP 415") {

        put("api/oppija", body = Json.write(makeOppija(defaultHenkilö, List(defaultOpiskeluoikeus))), headers = authHeaders() ++ Map(("Content-type" -> "text/plain"))) {
          verifyResponseStatus(415, KoskiErrorCategory.unsupportedMediaType.jsonOnly("Wrong content type: only application/json content type with UTF-8 encoding allowed"))
        }
      }
    }

    describe("Omien tietojen muokkaaminen") {
      it("On estetty") {
        putOpiskeluOikeus(opiskeluoikeus(oppilaitos = Oppilaitos(omnia), tutkinto = AmmatillinenExampleData.autoalanPerustutkinnonSuoritus(Oppilaitos(omnia))),
                          henkilö = MockOppijat.omattiedot,
                          headers = authHeaders(MockUsers.omattiedot) ++ jsonContent) {
          verifyResponseStatus(403, KoskiErrorCategory.forbidden.omienTietojenMuokkaus())
        }
      }
    }

    describe("Henkilötiedot") {
      describe("Nimenä tyhjä merkkijono") {
        it("palautetaan HTTP 400 virhe" ) (putHenkilö(defaultHenkilö.copy(sukunimi = "")) (verifyResponseStatus(400)))
      }


      describe("Hetun ollessa") {
        describe("muodoltaan virheellinen") {
          it("palautetaan HTTP 400 virhe" ) (putHenkilö(defaultHenkilö.copy(hetu = "010101-123123"))
            (verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.henkilötiedot.hetu("Virheellinen muoto hetulla: 010101-123123"))))
        }
        describe("muodoltaan oikea, mutta väärä tarkistusmerkki") {
          it("palautetaan HTTP 400 virhe" ) (putHenkilö(defaultHenkilö.copy(hetu = "010101-123P"))
            (verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.henkilötiedot.hetu("Virheellinen tarkistusmerkki hetussa: 010101-123P"))))
        }
        describe("päivämäärältään tulevaisuudessa") {
          it("palautetaan HTTP 400 virhe" ) (putHenkilö(defaultHenkilö.copy(hetu = "141299A903C"))
            (verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.henkilötiedot.hetu("Syntymäpäivä hetussa: 141299A903C on tulevaisuudessa"))))
        }
        describe("päivämäärältään virheellinen") {
          it("palautetaan HTTP 400 virhe" ) (putHenkilö(defaultHenkilö.copy(hetu = "300215-123T"))
            (verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.henkilötiedot.hetu("Virheellinen syntymäpäivä hetulla: 300215-123T"))))
        }
        describe("validi") {
          it("palautetaan HTTP 200" ) (putHenkilö(defaultHenkilö.copy(hetu = "010101-123N"))
            (verifyResponseStatus(200)))
        }
      }

      describe("Käytettäessä oppijan oidia") {
        describe("Oid ok") {
          it("palautetaan HTTP 200" ) (putHenkilö(OidHenkilö(MockOppijat.eero.oid)) (verifyResponseStatus(200)))
        }

        describe("Oid virheellinen") {
          it("palautetaan HTTP 400" ) (putHenkilö(OidHenkilö("123.123.123")) (verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.jsonSchema(".*ECMA 262 regex.*".r))))
        }

        describe("Oppijaa ei löydy oidilla") {
          it("palautetaan HTTP 404" ) (putHenkilö(OidHenkilö("1.2.246.562.24.19999999999")) (verifyResponseStatus(404, KoskiErrorCategory.notFound.oppijaaEiLöydy("Oppijaa 1.2.246.562.24.19999999999 ei löydy."))))
        }
      }

      describe("Käytettäessä oppijan kaikkia tietoja") {
        describe("Oid ok") {
          it("palautetaan HTTP 200" ) (putHenkilö(MockOppijat.eero) (verifyResponseStatus(200)))
        }

        describe("Oid virheellinen") {
          it("palautetaan HTTP 400" ) (putHenkilö(TäydellisetHenkilötiedot("123.123.123", "010101-123N", "Testi", "Testi", "Toivola", None, None)) (verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.jsonSchema(".*ECMA 262 regex.*".r))))
        }
      }
    }

    describe("Opiskeluoikeudet") {
      describe("Jos lähetetään 0 opiskeluoikeutta") {
        it("palautetaan HTTP 400") {
          putOppija(Oppija(defaultHenkilö, List())) {
            verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.tyhjäOpiskeluoikeusLista("Annettiin tyhjä lista opiskeluoikeuksia."))
          }
        }
      }

      describe("Päivitettäessä opiskeluoikeus käyttäen sen id:tä") {
        it("Id ok") {
          val opiskeluoikeus = lastOpiskeluOikeus(MockOppijat.eero.oid)
          putOppija(Oppija(MockOppijat.eero, List(opiskeluoikeus))) {
            verifyResponseStatus(200)
          }
        }

        it("Tuntematon id") {
          val opiskeluoikeus = lastOpiskeluOikeus(MockOppijat.eero.oid)
          putOppija(Oppija(MockOppijat.eero, List(opiskeluoikeus.withIdAndVersion(id = Some(0), versionumero = None)))) {
            verifyResponseStatus(404, KoskiErrorCategory.notFound.opiskeluoikeuttaEiLöydyTaiEiOikeuksia("Opiskeluoikeutta 0 ei löydy tai käyttäjällä ei ole oikeutta sen katseluun"))
          }
        }
      }
    }

    describe("Oppilaitos") {
      def oppilaitoksella(oid: String) = defaultOpiskeluoikeus.copy(oppilaitos = Oppilaitos(oid))

      describe("Kun opinto-oikeutta yritetään lisätä oppilaitokseen, johon käyttäjällä ei ole oikeuksia") {
        it("palautetaan HTTP 403 virhe" ) { putOpiskeluOikeus(oppilaitoksella("1.2.246.562.10.93135224694"), headers = authHeaders(MockUsers.omniaPalvelukäyttäjä) ++ jsonContent) (
          verifyResponseStatus(403, KoskiErrorCategory.forbidden.organisaatio("Ei oikeuksia organisatioon 1.2.246.562.10.93135224694")))
        }
      }

      describe("Kun opinto-oikeutta yritetään lisätä oppilaitokseen, joka ei ole oppilaitos") {
        it("palautetaan HTTP 400 virhe" ) { putOpiskeluOikeus(oppilaitoksella("1.2.246.562.10.346830761110")) (
          verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.organisaatio.vääränTyyppinen("Organisaatio 1.2.246.562.10.346830761110 ei ole Oppilaitos")))
        }
      }

      describe("Kun opinto-oikeutta yritetään lisätä oppilaitokseen, jota ei löydy organisaatiopalvelusta") {
        it("palautetaan HTTP 400 virhe" ) { putOpiskeluOikeus(oppilaitoksella("1.2.246.562.10.146810761111")) (
          verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.organisaatio.tuntematon("Organisaatiota 1.2.246.562.10.146810761111 ei löydy organisaatiopalvelusta")))
        }
      }

      describe("Kun oppilaitoksen oid on virheellistä muotoa") {
        it("palautetaan HTTP 400 virhe" ) { putOpiskeluOikeus(oppilaitoksella("asdf")) (
          verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.jsonSchema(".*ECMA 262 regex.*".r)))
        }
      }
    }

    describe("Suorituksen toimipiste") {
      def toimipisteellä(oid: String) = defaultOpiskeluoikeus.copy(suoritukset = List(autoalanPerustutkinnonSuoritus().copy(toimipiste = OidOrganisaatio(oid))))

      describe("Kun yritetään käyttää toimipistettä, johon käyttäjällä ei ole oikeuksia") {
        it("palautetaan HTTP 403 virhe" ) { putOpiskeluOikeus(toimipisteellä(MockOrganisaatiot.helsinginKaupunki)) (
          verifyResponseStatus(403, KoskiErrorCategory.forbidden.organisaatio("Ei oikeuksia organisatioon " + MockOrganisaatiot.helsinginKaupunki)))
        }
      }
    }

    describe("Opiskeluoikeuden päivämäärät") {
      describe("Alkaminen ja päättyminen") {
        it("Päivämäärät kunnossa -> palautetaan HTTP 200") {
          putOpiskeluOikeus(defaultOpiskeluoikeus.copy(arvioituPäättymispäivä = Some(date(2018, 5, 31))))(verifyResponseStatus(200))
        }
        it("Päivämääräformaatti virheellinen -> palautetaan HTTP 400") {
          putOpiskeluOikeusWithSomeMergedJson(Map("alkamispäivä" -> "2015.01-12")){
            verifyResponseStatus(400, KoskiErrorCategory.badRequest.format.pvm("Virheellinen päivämäärä: 2015.01-12"))
          }
        }
        it("Päivämäärä virheellinen -> palautetaan HTTP 400") {
          putOpiskeluOikeusWithSomeMergedJson(Map("alkamispäivä" -> "2015-01-32")){
            verifyResponseStatus(400, KoskiErrorCategory.badRequest.format.pvm("Virheellinen päivämäärä: 2015-01-32"))
          }
        }

        describe("Väärä päivämääräjärjestys") {
          it("alkamispäivä > päättymispäivä" ) (putOpiskeluOikeus(päättymispäivällä(defaultOpiskeluoikeus, date(1999, 5, 31))) {
            verifyResponseStatus(400,
              KoskiErrorCategory.badRequest.validation.date.loppuEnnenAlkua("alkamispäivä (2000-01-01) oltava sama tai aiempi kuin päättymispäivä(1999-05-31)"),
              KoskiErrorCategory.badRequest.validation.date.jaksojenJärjestys("tila.opiskeluoikeusjaksot: 2000-01-01 oltava sama tai aiempi kuin 1999-05-31")
            )
          })

          it("alkamispäivä > arvioituPäättymispäivä" ) (putOpiskeluOikeus(defaultOpiskeluoikeus.copy(arvioituPäättymispäivä = Some(date(1999, 5, 31)))){
            verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.date.loppuEnnenAlkua("alkamispäivä (2000-01-01) oltava sama tai aiempi kuin arvioituPäättymispäivä(1999-05-31)"))
          })
        }

        describe("Päivämäärät vs opiskeluoikeusjaksot") {
          it("alkamispäivä puuttuu, vaikka opiskeluoikeusjakso on olemassa") { putOpiskeluOikeus(defaultOpiskeluoikeus.copy(alkamispäivä = None)) {
            verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.date.alkamispäivä("Opiskeluoikeuden alkamispäivä (null) ei vastaa ensimmäisen opiskeluoikeusjakson alkupäivää (2000-01-01)"))
          }}
          it("alkamispäivä ei vastaa ensimmäisen opiskeluoikeusjakson päivämäärää") { putOpiskeluOikeus(defaultOpiskeluoikeus.copy(alkamispäivä = Some(date(1999, 12, 31)))) {
            verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.date.alkamispäivä("Opiskeluoikeuden alkamispäivä (1999-12-31) ei vastaa ensimmäisen opiskeluoikeusjakson alkupäivää (2000-01-01)"))
          }}
          it("päättymispäivä on annettu, vaikka viimeinen opiskeluoikeus on tilassa Läsnä") { putOpiskeluOikeus(defaultOpiskeluoikeus.copy(päättymispäivä = Some(date(2010, 12, 31)))) {
            verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.date.päättymispäivämäärä("Opiskeluoikeuden päättymispäivä (2010-12-31) ei vastaa opiskeluoikeuden päättävän opiskeluoikeusjakson alkupäivää (null)"))
          }}
          it("päättymispäivä ei vastaa opiskelut päättävän jakson päivää") { putOpiskeluOikeus(päättymispäivällä(defaultOpiskeluoikeus, date(2010, 12, 30)).copy(päättymispäivä = Some(date(2010, 12, 31)))) {
            verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.date.päättymispäivämäärä("Opiskeluoikeuden päättymispäivä (2010-12-31) ei vastaa opiskeluoikeuden päättävän opiskeluoikeusjakson alkupäivää (2010-12-30)"))
          }}
        }
      }

      describe("Läsnäolojaksot") {
        describe("Päivämäärät kunnossa") {
          it("palautetaan HTTP 200") (putOpiskeluOikeus(defaultOpiskeluoikeus.copy(läsnäolotiedot = Some(YleisetLäsnäolotiedot(List(
            YleinenLäsnäolojakso(date(2015, 8, 1), "lasna"),
            YleinenLäsnäolojakso(date(2016, 1, 1), "poissa"),
            YleinenLäsnäolojakso(date(2016, 5, 21), "lasna")
          ))))
          )(verifyResponseStatus(200)))
        }
        describe("jaksojen päivämääräjärjestys on väärä") {
          it("palautetaan HTTP 400") (putOpiskeluOikeus(defaultOpiskeluoikeus.copy(läsnäolotiedot = Some(YleisetLäsnäolotiedot(List(
            YleinenLäsnäolojakso(date(2015, 8, 1), "lasna"),
            YleinenLäsnäolojakso(date(2015, 7, 1), "poissa")
          ))))) {
            verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.date.jaksojenJärjestys("läsnäolotiedot.läsnäolojaksot: 2015-08-01 oltava sama tai aiempi kuin 2015-07-01"))
          })
        }
      }
    }

    describe("Lokalisoidut tekstit") {
      def withName(name: LocalizedString) = defaultOpiskeluoikeus.copy(tyyppi = defaultOpiskeluoikeus.tyyppi.copy(nimi = Some(name)))

      it("suomi riittää") {
        putOpiskeluOikeus(withName(LocalizedString.finnish("Jotain"))) {
          verifyResponseStatus(200)
        }
      }
      it("ruotsi riittää") {
        putOpiskeluOikeus(withName(LocalizedString.swedish("Något"))) {
          verifyResponseStatus(200)
        }
      }
      it("englanti riittää") {
        putOpiskeluOikeus(withName(LocalizedString.english("Something"))) {
          verifyResponseStatus(200)
        }
      }

      it("vähintään yksi kieli vaaditaan") {
        putOpiskeluOikeusWithSomeMergedJson(Map("tyyppi" -> Map("nimi" -> Map()))) {
          verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.jsonSchema(".*object has missing required properties.*".r))
        }
      }
    }
  }

  def putOpiskeluOikeusWithSomeMergedJson[A](opiskeluOikeus: JValue, henkilö: Henkilö = defaultHenkilö, headers: Headers = authHeaders() ++ jsonContent)(f: => A): A = {
    putOppija(makeOppija(henkilö, List(Json.toJValue(defaultOpiskeluoikeus).merge(opiskeluOikeus))), headers)(f)
  }

}
