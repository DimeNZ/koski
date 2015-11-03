package fi.oph.tor.eperusteet

import org.json4s.JsonAST.JObject
import org.json4s._
import org.json4s.reflect.TypeInfo

case class EPerusteRakenne(id: Long, nimi: Map[String, String], diaarinumero: String, koulutustyyppi: String, koulutukset: List[EPerusteKoulutus], suoritustavat: List[ESuoritustapa], tutkinnonOsat: List[ETutkinnonOsa], osaamisalat: List[EOsaamisala])

case class ESuoritustapa(suoritustapakoodi: String, rakenne: ERakenneOsa, tutkinnonOsaViitteet: List[ETutkinnonOsaViite])
case class ETutkinnonOsaViite(id: Long, _tutkinnonOsa: String)
case class EOsaamisala(nimi: Map[String, String], arvo: String)
case class EOsaamisalaViite(osaamisalakoodiArvo: String)
case class ETutkinnonOsa(id: Long, nimi: Map[String, String], koodiArvo: String)

sealed trait ERakenneOsa
case class ERakenneModuuli(nimi: Option[Map[String, String]], osat: List[ERakenneOsa], osaamisala: Option[EOsaamisalaViite]) extends ERakenneOsa
case class ERakenneTutkinnonOsa(_tutkinnonOsaViite: String) extends ERakenneOsa

class RakenneOsaSerializer extends Serializer[ERakenneOsa] {
  private val PieceClass = classOf[ERakenneOsa]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), ERakenneOsa] = {
    case (TypeInfo(PieceClass, _), json) => json match {
      case moduuli: JObject if moduuli.values.contains("osat") => moduuli.extract[ERakenneModuuli]
      case osa: JObject => osa.extract[ERakenneTutkinnonOsa]
    }
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = PartialFunction.empty
}