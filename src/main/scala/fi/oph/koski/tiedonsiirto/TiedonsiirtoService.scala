package fi.oph.koski.tiedonsiirto

import java.time.LocalDateTime

import fi.oph.koski.db.Tables.TiedonsiirtoRow
import fi.oph.koski.json.Json
import fi.oph.koski.json.Json._
import fi.oph.koski.koskiuser.KoskiUser
import fi.oph.koski.log.KoskiMessageField._
import fi.oph.koski.log.KoskiOperation._
import fi.oph.koski.log.{AuditLog, AuditLogMessage}
import fi.oph.koski.oppija.OppijaRepository
import fi.oph.koski.organisaatio.OrganisaatioRepository
import fi.oph.koski.schema.{HenkilötiedotJaOid, Koodistokoodiviite, OidHenkilö, OrganisaatioWithOid}
import fi.oph.koski.util.DateOrdering
import org.json4s.JsonAST.{JArray, JString, JValue}
import org.json4s.{JValue, _}

class TiedonsiirtoService(tiedonsiirtoRepository: TiedonsiirtoRepository, organisaatioRepository: OrganisaatioRepository, oppijaRepository: OppijaRepository) {
  def kaikkiTiedonsiirrot(koskiUser: KoskiUser): List[HenkilönTiedonsiirrot] = toHenkilönTiedonsiirrot(findAll(koskiUser))

  def virheelliset(koskiUser: KoskiUser): List[HenkilönTiedonsiirrot] =
    kaikkiTiedonsiirrot(koskiUser).filter { siirrot =>
      siirrot.rivit.groupBy(_.oppilaitos).exists { case (_, rivit) => rivit.headOption.exists(_.virhe.isDefined) }
    }.map(v => v.copy(rivit = v.rivit.filter(_.virhe.isDefined)))


  def storeTiedonsiirtoResult(implicit koskiUser: KoskiUser, oppijaOid: Option[OidHenkilö], data: Option[JValue], error: Option[TiedonsiirtoError]) {
    if (!koskiUser.isPalvelukäyttäjä) {
      return
    }

    val oppija = data.flatMap(extractHenkilö(_, oppijaOid))

    val oppilaitokset = data.map(_ \ "opiskeluoikeudet" \ "oppilaitos" \ "oid").collect {
      case JArray(oids) => oids.collect { case JString(oid) => oid }
      case JString(oid) => List(oid)
    }.map(_.flatMap(organisaatioRepository.getOrganisaatio)).map(toJValue)

    koskiUser.juuriOrganisaatio.foreach(org => tiedonsiirtoRepository.create(koskiUser.oid, org.oid, oppija, oppilaitokset, error))
  }

  private def extractHenkilö(data: JValue, oidHenkilö: Option[OidHenkilö])(implicit user: KoskiUser): Option[JValue] = {
    val annetutHenkilötiedot: JValue = data \ "henkilö"
    val annettuTunniste: HetuTaiOid = Json.fromJValue[HetuTaiOid](annetutHenkilötiedot)
    val oid: Option[String] = oidHenkilö.map(_.oid).orElse(annettuTunniste.oid)
    val haetutTiedot: Option[HenkilötiedotJaOid] = (oid, annettuTunniste.hetu) match {
      case (Some(oid), None) => oppijaRepository.findByOid(oid).map(_.toHenkilötiedotJaOid)
      case (None, Some(hetu)) => oppijaRepository.findOppijat(hetu).headOption
      case _ => None
    }
    haetutTiedot.map(toJValue).orElse(oidHenkilö match {
      case Some(oidHenkilö) => Some(annetutHenkilötiedot.merge(toJValue(oidHenkilö)))
      case None => annetutHenkilötiedot.toOption
    })
  }

  private def toHenkilönTiedonsiirrot(tiedonsiirrot: Seq[TiedonsiirtoRow]) = {
    implicit val ordering = DateOrdering.localDateTimeReverseOrdering
    tiedonsiirrot.groupBy { t =>
      val oppijanTunniste = t.oppija.map(Json.fromJValue[HetuTaiOid])
      oppijanTunniste.flatMap(_.hetu).orElse(oppijanTunniste.map(_.oid))
    }.map {
      case (_, rows) =>
        val oppija = rows.head.oppija.flatMap(_.extractOpt[Henkilö])
        val rivit = rows.map { row =>
          val oppilaitos = row.oppilaitos.flatMap(_.extractOpt[List[OrganisaatioWithOid]])
          TiedonsiirtoRivi(row.aikaleima.toLocalDateTime, oppija, oppilaitos, row.virheet, row.data)
        }
        HenkilönTiedonsiirrot(oppija, rivit.sortBy(_.aika))
    }.toList.sortBy(_.rivit.head.aika)
  }

  private def findAll(koskiUser: KoskiUser): Seq[TiedonsiirtoRow] = {
    AuditLog.log(AuditLogMessage(TIEDONSIIRTO_KATSOMINEN, koskiUser, Map(juuriOrganisaatio -> koskiUser.juuriOrganisaatio.map(_.oid).getOrElse("ei juuriorganisaatiota"))))
    tiedonsiirtoRepository.findByOrganisaatio(koskiUser)
  }

}

case class HenkilönTiedonsiirrot(oppija: Option[Henkilö], rivit: Seq[TiedonsiirtoRivi])
case class TiedonsiirtoRivi(aika: LocalDateTime, oppija: Option[Henkilö], oppilaitos: Option[List[OrganisaatioWithOid]], virhe: Option[AnyRef], inputData: Option[AnyRef])
case class Henkilö(oid: Option[String], hetu: Option[String], etunimet: Option[String], kutsumanimi: Option[String], sukunimi: Option[String], äidinkieli: Option[Koodistokoodiviite])
case class HetuTaiOid(oid: Option[String], hetu: Option[String])
