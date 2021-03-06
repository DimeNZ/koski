package fi.oph.koski.documentation

import com.tristanhunt.knockoff.DefaultDiscounter._
import fi.oph.koski.http.ErrorCategory
import fi.oph.koski.schema.KoskiSchema
import fi.oph.scalaschema.ClassSchema

import scala.xml.Elem

object KoskiTiedonSiirtoHtml {
  private val schemaViewerUrl = "/koski/json-schema-viewer#koski-oppija-schema.json"
  private val schemaFileUrl = "/koski/documentation/koski-oppija-schema.json"
  private def general =s"""

# Koski-tiedonsiirtoprotokolla

Tässä dokumentissa kuvataan Koski-järjestelmän tiedonsiirrossa käytettävä protokolla. Lisätietoja Koski-järjestelmästä löydät [Opetushallituksen wiki-sivustolta](https://confluence.csc.fi/display/OPHPALV/Koski). Järjestelmän lähdekoodit ja kehitysdokumentaatio [Githubissa](https://github.com/Opetushallitus/koski).

Protokolla, kuten Koski-järjestelmäkin, on työn alla, joten kaikki voi vielä muuttua.

Muutama perusasia tullee kuitenkin säilymään:

- Rajapinnan avulla järjestelmään voi tallentaa tietoja oppijoiden opinto-oikeuksista, opintosuorituksista ja läsnäolosta oppilaitoksissa
- Rajapinnan avulla tietoja voi myös hakea ja muokata
- Rajapinnan käyttö vaatii autentikoinnin ja pääsy tietoihin rajataan käyttöoikeusryhmillä.
  Näin ollen esimerkiksi oikeus oppilaan tietyssä oppilaitoksessa suorittamien opintojen päivittämiseen voidaan antaa kyseisen oppilaitoksen henkilöstölle
- Rajapinta mahdollistaa myös automaattiset tiedonsiirrot tietojärjstelmien välillä. Näin esimerkiksi tietyt viranomaiset voivat saada tietoja Koskesta.
  Samoin oppilaitoksen tietojärjestelmät voivat päivittää tietoja Koskeen.
- Järjestelmä tarjoaa REST-tyyppisen tiedonsiirtorajapinnan, jossa dataformaattina on JSON
- Samaa tiedonsiirtoprotokollaa ja dataformaattia pyritään soveltuvilta osin käyttämään sekä käyttöliittymille,
  jotka näyttävät tietoa loppukäyttäjille, että järjestelmien väliseen kommunikaatioon

## JSON-dataformaatti

Käytettävästä JSON-formaatista on laadittu työversio, jonka toivotaan vastaavan ammatillisen koulutuksen tarpeisiin.
Tällä formaatilla siis tulisi voida siirtää tietoja ammatillista koulutusta tarjoavien koulutustoimijoiden tietojärjestelmistä Koskeen ja eteenpäin
tietoja tarvitsevien viramomaisten järjestelmiin ja loppukäyttäjiä, kuten oppilaitosten virkailijoita palveleviin käyttöliittymiin.
Formaattia on tarkoitus laajentaa soveltumaan myös muiden koulutustyyppien tarpeisiin, mutta näitä tarpeita ei ole vielä riittävällä tasolla kartoitettu,
jotta konkreettista dataformaattia voitaisiin suunnitella. Yksi formaatin suunnittelukriteereistä on toki ollut sovellettavuus muihinkin koulutustyyppeihin.

### JSON Schema

Käytettävä JSON-dataformaatti on kuvattu [JSON-schemalla](http://json-schema.org/), jota vasten siirretyt tiedot voidaan myös automaattisesti validoida.

<div class="preview-image-links">
  <a href="${schemaViewerUrl}">
    <image src="/koski/images/tor-schema-preview.png">
    <div class="caption">Visualisoitu JSON-schema</div>
    <p>Voi tarkastella schemaa visualisointityökalun avulla. Tällä työkalulla voi myös validoida JSON-viestejä schemaa vasten. Klikkaamalla kenttiä saat näkyviin niiden tarkemmat kuvaukset.</p>
  </a>
  <a href="${schemaFileUrl}">
    <image src="/koski/images/tor-schema-json-preview.png">
    <div class="caption">Lataa JSON-tiedostona</div>
    <p>Voit myös ladata scheman tiedostona</p>
  </a>
</div>

Tietokentät, joissa validit arvot on lueteltavissa, on kooditettu käyttäen hyväksi Opintopolku-järjestelmään kuuluvaa [Koodistopalvelua](https://github.com/Opetushallitus/koodisto).
Esimerkki tällaisesta kentästä on tutkintoon johtavan koulutuksen [koulutuskoodi](/koski/documentation/koodisto/koulutus/latest).

Scalaa osaaville ehkä nopein tapa tutkia tietomallia on kuitenkin sen lähdekoodi. Githubista löytyy sekä [scheman](https://github.com/Opetushallitus/koski/blob/master/src/main/scala/fi/oph/koski/schema/Oppija.scala),
että [esimerkkien](https://github.com/Opetushallitus/koski/blob/master/src/main/scala/fi/oph/koski/documentation/Examples.scala) lähdekoodit.

"""

  def rest_apis ="""

## REST-rajapinnat

Kaikki rajapinnat vaativat HTTP Basic Authentication -tunnistautumisen, eli käytännössä `Authorization`-headerin HTTP-pyyntöön.

Rajapinnat on lueteltu ja kuvattu alla. Voit myös testata rajapintojen toimintaa tällä sivulla, kunhan käyt ensin [kirjautumassa sisään](/koski) järjestelmään.
Saat tarvittavat tunnukset Koski-kehitystiimiltä pyydettäessä.

Rajapintojen käyttämät virhekoodit on myös kuvattu alla. Virhetapauksissa rajapinnat käyttävät alla kuvattuja HTTP-statuskoodeja ja sisällyttävät tarkemmat virhekoodit ja selitteineen JSON-tyyppiseen paluuviestiin.
Samaan virhevastaukseen voi liittyä useampi virhekoodi/selite.

  """

  def html = {
    <html>
      <head>
        <meta charset="UTF-8"></meta>
        <link rel="stylesheet" type="text/css" href="css/documentation.css"></link>
        <link rel="stylesheet" href="//cdnjs.cloudflare.com/ajax/libs/highlight.js/9.1.0/styles/default.min.css"/>
        <link rel="stylesheet" type="text/css" href="/koski/css/codemirror/codemirror.css"/>
        <script src="//cdnjs.cloudflare.com/ajax/libs/highlight.js/9.1.0/highlight.min.js"></script>
        <script src="/koski/js/codemirror/codemirror.js"></script>
        <script src="/koski/js/codemirror/javascript.js"></script>
      </head>
      <body>
        <header><div class="logo"/></header>
        <div class="content">
          <section>
            {toXHTML( knockoff(general) )}
          </section>
          <section>
            {toXHTML( knockoff(rest_apis) )}
            { ApiTesterHtml.apiOperationsHtml }
          </section>
        <section>
          <h2>Esimerkkidata annotoituna</h2>
          <p>
            Toinen hyvä tapa tutustua tiedonsiirtoprotokollaan on tutkia esimerkkiviestejä.
            Alla joukko viestejä, joissa oppijan opinnot ovat eri vaiheissa. Kussakin esimerkissa on varsinaisen JSON-sisällön lisäksi schemaan pohjautuva annotointi ja linkitykset koodistoon ja OKSA-sanastoon.
          </p>
          { examplesHtml(ExamplesPerusopetukseenValmistavaOpetus.examples, "Perusopetukseen valmistava opetus") }
          { examplesHtml(ExamplesPerusopetus.examples, "Perusopetus") }
          { examplesHtml(ExamplesPerusopetuksenLisaopetus.examples, "Perusopetuksen lisäopetus") }
          { examplesHtml(ExamplesLukio.examples ++ ExamplesLukioonValmistavaKoulutus.examples, "Lukiokoulutus") }
          { examplesHtml(ExamplesIB.examples, "IB-koulutus") }
          { examplesHtml(ExamplesAmmatillinen.examples, "Ammatillinen koulutus") }
          { examplesHtml(ExamplesValma.examples ++ ExamplesTelma.examples, "Valmentava koulutus") }
          { examplesHtml(ExamplesKorkeakoulu.examples, "Korkeakoulu (Virrasta)") }
          { examplesHtml(ExamplesYlioppilastutkinto.examples, "Ylioppilastutkinto (Ylioppilastutkintorekisteristä)") }
        </section>
        </div>
        <script src="js/polyfills/promise.js"></script>
        <script src="js/polyfills/fetch.js"></script>
        <script src="js/polyfills/dataset.js"></script>
        <script src="js/documentation/api-operations.js"></script>
        <script src="js/documentation/json-examples.js"></script>
      </body>
    </html>
  }

  def examplesHtml(examples: List[Example], title: String) = {
    <h3>{title}</h3>
    <ul class="example-list">
      {
        examples.map { example: Example =>
          <li class="example-item">
            <a class="example-link">
              {example.description}
            </a>
            <a class="example-as-json" href={"/koski/documentation/examples/" + example.name + ".json"} target="_blank">lataa JSON</a>
            <table class="json">
              {SchemaToJsonHtml.buildHtml(KoskiSchema.schema.asInstanceOf[ClassSchema], example.data)}
            </table>
          </li>
        }
      }
    </ul>
  }
}