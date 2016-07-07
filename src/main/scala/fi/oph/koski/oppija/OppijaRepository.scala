package fi.oph.koski.oppija

import fi.oph.koski.cache.{CacheDetails, CachingProxy, CachingStrategy}
import fi.oph.koski.henkilo.{AuthenticationServiceClient, CreateUser, User, UserQueryUser}
import fi.oph.koski.http.HttpStatus
import fi.oph.koski.koodisto.KoodistoViitePalvelu
import fi.oph.koski.koskiuser.KoskiUser
import fi.oph.koski.log.{Logging, TimedProxy}
import fi.oph.koski.schema._
import fi.oph.koski.util.Invocation
import fi.oph.koski.virta.{VirtaAccessChecker, VirtaClient, VirtaOppijaRepository}
import fi.oph.koski.ytr.{YlioppilasTutkintoRekisteri, YtrAccessChecker, YtrOppijaRepository}

trait OppijaRepository extends Logging {
  def findOppijat(query: String)(implicit user: KoskiUser): List[HenkilötiedotJaOid]
  def findByOid(oid: String)(implicit user: KoskiUser): Option[TäydellisetHenkilötiedot]
  def findByOids(oids: List[String])(implicit user: KoskiUser): List[TäydellisetHenkilötiedot]
  def resetFixtures {}
  def findOrCreate(henkilö: UusiHenkilö)(implicit user: KoskiUser): Either[HttpStatus, Henkilö.Oid]
}

object OppijaRepository {
  def apply(authenticationServiceClient: AuthenticationServiceClient, koodistoViitePalvelu: KoodistoViitePalvelu, virtaClient: VirtaClient, virtaAccessChecker: VirtaAccessChecker, ytr: YlioppilasTutkintoRekisteri, ytrAccessChecker: YtrAccessChecker) = {
    val opintopolku = new OpintopolkuOppijaRepository(authenticationServiceClient, koodistoViitePalvelu)
    CachingProxy(OppijaRepositoryCachingStrategy, TimedProxy(
      CompositeOppijaRepository(List(
        TimedProxy(opintopolku.asInstanceOf[OppijaRepository]),
        TimedProxy(VirtaOppijaRepository(virtaClient, opintopolku, virtaAccessChecker).asInstanceOf[OppijaRepository]),
        TimedProxy(YtrOppijaRepository(ytr, opintopolku, ytrAccessChecker).asInstanceOf[OppijaRepository])
      )).asInstanceOf[OppijaRepository]
    ))
  }
}

object OppijaRepositoryCachingStrategy extends CachingStrategy("OppijaRepository", new CacheDetails {
  def durationSeconds = 60
  def maxSize = 100
  def refreshing = true
  override def storeValuePredicate: (Invocation, AnyRef) => Boolean = {
    case (invocation, value) => invocation.f.name match {
      case "findByOid" => value match {
        case Some(_) => true
        case _ => false
      }
      case _ => false
    }
  }})
