package fi.oph.tor.organisaatio

import fi.oph.tor.http.HttpStatus
import fi.oph.tor.json.{ContextualExtractor, Json}
import fi.oph.tor.schema._
import fi.vm.sade.utils.slf4j.Logging
import org.json4s._
import org.json4s.reflect.TypeInfo

object OrganisaatioResolvingDeserializer extends Deserializer[Organisaatio] with Logging {
  val organisaatioClasses = List(classOf[Organisaatio], classOf[OidOrganisaatio], classOf[Tutkintotoimikunta], classOf[Yritys])

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), Organisaatio] = {
    case (TypeInfo(c, _), json) if organisaatioClasses.contains(c) =>
      OrganisaatioDeserializer.deserialize(Json.jsonFormats)((TypeInfo(classOf[Organisaatio], None), json)) match {
        case OidOrganisaatio(oid, _) =>
          ContextualExtractor.getContext[{def organisaatioRepository: OrganisaatioRepository}] match {
            case Some(context) => context.organisaatioRepository.getOrganisaatio(oid) match {
              case Some(org) => OidOrganisaatio(org.oid, Some(org.nimi))
              case None => ContextualExtractor.extractionError(HttpStatus.badRequest("Organisaatiota " + oid + " ei löydy organisaatiopalvelusta"))
            }
          }
        case org => org
      }
  }
}