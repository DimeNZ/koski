package fi.oph.koski.virta

import fi.oph.koski.cache.{Cache, CacheManager, KeyValueCache}
import fi.oph.koski.http.KoskiErrorCategory
import fi.oph.koski.koodisto.KoodistoViitePalvelu
import fi.oph.koski.koski.KoskiValidator
import fi.oph.koski.koskiuser.{AccessChecker, AccessType, KoskiUser}
import fi.oph.koski.log.Logging
import fi.oph.koski.opiskeluoikeus.AuxiliaryOpiskeluOikeusRepository
import fi.oph.koski.oppija.OppijaRepository
import fi.oph.koski.oppilaitos.OppilaitosRepository
import fi.oph.koski.schema.{Opiskeluoikeus, _}

abstract class HetuBasedOpiskeluoikeusRepository[OO <: Opiskeluoikeus](oppijaRepository: OppijaRepository, oppilaitosRepository: OppilaitosRepository, koodistoViitePalvelu: KoodistoViitePalvelu, accessChecker: AccessChecker, validator: Option[KoskiValidator] = None)(implicit cacheInvalidator: CacheManager) extends AuxiliaryOpiskeluOikeusRepository with Logging {
  def opiskeluoikeudetByHetu(hetu: String): List[OO]

  // hetu -> org.oids cache for filtering only
  private val organizationsCache = KeyValueCache[Henkilö.Hetu, List[Organisaatio.Oid]](Cache.cacheAllNoRefresh(getClass.getSimpleName + ".organisations", 3600, 100000), doFindOrgs)
  private val cache = KeyValueCache[Henkilö.Hetu, List[OO]](Cache.cacheAllNoRefresh(getClass.getSimpleName + ".opiskeluoikeudet", 3600, 100), doFindByHenkilö)

  def doFindOrgs(hetu: Henkilö.Hetu): List[Organisaatio.Oid] = {
    cache(hetu).map(_.oppilaitos.oid)
  }

  def doFindByHenkilö(hetu: Henkilö.Hetu): List[OO] = {
    try {
      val opiskeluoikeudet = opiskeluoikeudetByHetu(hetu)

      opiskeluoikeudet flatMap { opiskeluoikeus =>
        val oppija = Oppija(UusiHenkilö(hetu, "tuntematon", "tuntematon", "tuntematon"), List(opiskeluoikeus))
        validator match {
          case Some(validator) =>
            validator.validateAsJson(oppija)(KoskiUser.systemUser, AccessType.read) match {
              case Right(oppija) =>
                Some(opiskeluoikeus)
              case Left(status) =>
                if (status.errors.map(_.key).contains(KoskiErrorCategory.badRequest.validation.jsonSchema.key)) {
                  logger.error("Ulkoisesta järjestelmästä saatu opiskeluoikeus ei ole validi Koski-järjestelmän JSON schemassa: " + status)
                  None
                } else {
                  logger.warn("Ulkoisesta järjestelmästä saatu opiskeluoikeus sisältää validointivirheitä " + status)
                  Some(opiskeluoikeus)
                }
            }
          case None => Some(opiskeluoikeus)
        }
      }
    } catch {
      case e: Exception =>
        logger.error(e)(s"Failed to fetch data for $hetu")
        Nil
    }
  }
  private def getHenkilötiedot(oid: String)(implicit user: KoskiUser): Option[TäydellisetHenkilötiedot] = oppijaRepository.findByOid(oid)
  private def accessCheck[T](list: => List[T])(implicit user: KoskiUser): List[T] = if (accessChecker.hasAccess(user)) { list } else { Nil }
  private def findByHenkilö(henkilö: Henkilö with Henkilötiedot)(implicit user: KoskiUser): List[OO] = accessCheck(cache(henkilö.hetu).filter(oo => user.hasReadAccess(oo.oppilaitos.oid)))

  // Public methods
  def filterOppijat(oppijat: Seq[HenkilötiedotJaOid])(implicit user: KoskiUser): List[HenkilötiedotJaOid] = accessCheck(oppijat.par.filter(oppija => organizationsCache(oppija.hetu).filter(orgOid => user.hasReadAccess(orgOid)).nonEmpty).toList)
  def findByOppijaOid(oid: String)(implicit user: KoskiUser): List[Opiskeluoikeus] = accessCheck(getHenkilötiedot(oid).toList.flatMap(findByHenkilö(_)))
}
