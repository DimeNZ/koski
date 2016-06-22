package fi.oph.koski.todistus

import fi.oph.koski.localization.LocalizedString._
import fi.oph.koski.schema._

import scala.xml.NodeSeq.Empty
import scala.xml.{Elem, NodeSeq}

trait ValmentavanKoulutuksenTodistusHtml extends TodistusHtml {
  def koulutustoimija: Option[OrganisaatioWithOid]
  def oppilaitos: Oppilaitos
  def title: String
  def oppijaHenkilö: Henkilötiedot
  def todistus: Suoritus
  private def oppiaineet = todistus.osasuoritukset.toList.flatten

  def todistusHtml: Elem = {
    <html>
      <head>
        <link rel="stylesheet" type="text/css" href="/koski/css/todistus-common.css"></link>
        {styles}
      </head>
      <body>
        <div class="todistus">
          <h1>{title}</h1>
          <h2 class="koulutustoimija">{i(koulutustoimija.flatMap(_.nimi))}</h2>
          <h2 class="oppilaitos">{i(oppilaitos.nimi)}</h2>
          <h3 class="oppija">
            <span class="nimi">{oppijaHenkilö.sukunimi}, {oppijaHenkilö.etunimet}</span>
            <span class="hetu">{oppijaHenkilö.hetu}</span>
          </h3>
          <table class="tutkinnon-osat">
            {tutkinnonOtsikkoRivi}
            {tutkinnonOsat}
            <tr class="opintojen-laajuus">
              <td class="nimi">Opiskelijan suorittamien koulutuksen osien laajuus osaamispisteinä</td>
              <td class="laajuus">{decimalFormat.format(oppiaineet.map(laajuus).sum)}</td>
            </tr>
          </table>
          { todistus.vahvistus.toList.map(vahvistusHTML)}
        </div>
      </body>
    </html>
  }

  def styles: NodeSeq = Empty

  def tutkinnonOtsikkoRivi: Elem = <tr>
    <th class="oppiaine">Koulutuksen osat</th>
    <th class="laajuus">Suoritettu laajuus, osp</th>
    <th colspan="2" class="arvosana">Arvosana</th>
  </tr>

  def tutkinnonOsat = oppiaineet.groupBy(s => tyypinKuvaus(s.koulutusmoduuli)).toList.sortBy(_._1.get("fi")).flatMap { case (tyyppi, suoritukset: List[Suoritus]) =>
      <tr class="rakennemoduuli">
        <td class="oppiaine">{i(tyyppi)} {decimalFormat.format(suoritukset.map(laajuus).sum)} osp</td>
      </tr> :: tutkinnonOsaRivit(suoritukset)
  }

  def tutkinnonOsaRivit(suoritukset: List[Suoritus]): List[Elem] = suoritukset.map { oppiaine =>
    val nimiTeksti = i(oppiaine.koulutusmoduuli)
    <tr class="tutkinnon-osa">
      <td class="nimi">{nimiTeksti}</td>
      <td class="laajuus">{decimalFormat.format(laajuus(oppiaine))}</td>
      <td class="arvosana-kirjaimin">{i(oppiaine.arvosanaKirjaimin).capitalize}</td>
      <td class="arvosana-numeroin">{i(oppiaine.arvosanaNumeroin)}</td>
    </tr>
  }

  private def tyypinKuvaus(km: Koulutusmoduuli) = km match {
    case o: Valinnaisuus if o.pakollinen => finnish("Pakolliset koulutuksen osat")
    case _ => finnish("Valinnaiset koulutuksen osat")
  }
}