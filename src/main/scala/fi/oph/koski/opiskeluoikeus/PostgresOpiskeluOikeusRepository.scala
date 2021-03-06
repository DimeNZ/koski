package fi.oph.koski.opiskeluoikeus

import java.sql.SQLException

import fi.oph.koski.db.KoskiDatabase.DB
import fi.oph.koski.db.PostgresDriverWithJsonSupport.api._
import fi.oph.koski.db.Tables._
import fi.oph.koski.db._
import fi.oph.koski.history.OpiskeluoikeusHistoryRepository
import fi.oph.koski.http.{HttpStatus, KoskiErrorCategory}
import fi.oph.koski.json.Json
import fi.oph.koski.koski.{OpiskeluoikeusPäättynytAikaisintaan, OpiskeluoikeusPäättynytViimeistään, QueryFilter, TutkinnonTila}
import fi.oph.koski.koskiuser.KoskiUser
import fi.oph.koski.log.Logging
import fi.oph.koski.opiskeluoikeus.OpiskeluoikeusChangeValidator.validateOpiskeluoikeusChange
import fi.oph.koski.oppija.PossiblyUnverifiedOppijaOid
import fi.oph.koski.schema.Henkilö._
import fi.oph.koski.schema.Opiskeluoikeus.VERSIO_1
import fi.oph.koski.schema.{HenkilötiedotJaOid, KoskeenTallennettavaOpiskeluoikeus, Opiskeluoikeus, PäätasonSuoritus}
import fi.oph.koski.util.ReactiveStreamsToRx
import org.json4s.JArray
import rx.lang.scala.Observable
import slick.dbio.Effect.{Read, Transactional, Write}
import slick.dbio.{DBIOAction, NoStream}
import slick.lifted.Query
import slick.{dbio, lifted}

class PostgresOpiskeluOikeusRepository(val db: DB, historyRepository: OpiskeluoikeusHistoryRepository) extends OpiskeluOikeusRepository with GlobalExecutionContext with KoskiDatabaseMethods with Logging with SerializableTransactions {
  override def filterOppijat(oppijat: Seq[HenkilötiedotJaOid])(implicit user: KoskiUser) = {
    val query: lifted.Query[OpiskeluOikeusTable, OpiskeluOikeusRow, Seq] = for {
      oo <- OpiskeluOikeudetWithAccessCheck
      if oo.oppijaOid inSetBind oppijat.map(_.oid)
    } yield {
      oo
    }

    //logger.info(query.result.statements.head)

    val oppijatJoillaOpiskeluoikeuksia: Set[String] = runDbSync(query.map(_.oppijaOid).result).toSet

    oppijat.filter { oppija => oppijatJoillaOpiskeluoikeuksia.contains(oppija.oid)}
  }


  override def findByOppijaOid(oid: String)(implicit user: KoskiUser): Seq[Opiskeluoikeus] = {
    runDbSync(findByOppijaOidAction(oid).map(rows => rows.map(_.toOpiskeluOikeus)))
  }

  override def findByUserOid(oid: String)(implicit user: KoskiUser): Seq[Opiskeluoikeus] = {
    assert(oid == user.oid, "Käyttäjän oid: " + user.oid + " poikkeaa etsittävän oppijan oidista: " + oid)
    runDbSync(findAction(OpiskeluOikeudet.filter(_.oppijaOid === oid)).map(rows => rows.map(_.toOpiskeluOikeus)))
  }

  def findById(id: Int)(implicit user: KoskiUser): Option[(Opiskeluoikeus, String)] = {
    runDbSync(findAction(OpiskeluOikeudetWithAccessCheck.filter(_.id === id)).map(rows => rows.map(row => (row.toOpiskeluOikeus, row.oppijaOid)))).headOption
  }

  override def query(filters: List[QueryFilter])(implicit user: KoskiUser): Observable[(Oid, List[OpiskeluOikeusRow])] = {
    import ReactiveStreamsToRx._

    val query: Query[OpiskeluOikeusTable, OpiskeluOikeusRow, Seq] = filters.foldLeft(OpiskeluOikeudetWithAccessCheck.asInstanceOf[Query[OpiskeluOikeusTable, OpiskeluOikeusRow, Seq]]) {
      case (query, OpiskeluoikeusPäättynytAikaisintaan(päivä)) => query.filter(_.data.#>>(List("päättymispäivä")) >= päivä.toString)
      case (query, OpiskeluoikeusPäättynytViimeistään(päivä)) => query.filter(_.data.#>>(List("päättymispäivä")) <= päivä.toString)
      case (query, TutkinnonTila(tila)) => query.filter(_.data.#>>(List("suoritus", "tila", "koodiarvo")) === tila)
    }.sortBy(_.oppijaOid)

    // Note: it won't actually stream unless you use both `transactionally` and `fetchSize`. It'll collect all the data into memory.
    val rows: Observable[OpiskeluOikeusRow] = db.stream(query.result.transactionally.withStatementParameters(fetchSize = 1000)).publish.refCount

    val groupedByPerson: Observable[List[OpiskeluOikeusRow]] = rows
      .tumblingBuffer(rows.map(_.oppijaOid).distinctUntilChanged.drop(1))
      .map(_.toList)

    groupedByPerson.flatMap {
      case oikeudet@(firstRow :: _) =>
        val oppijaOid = firstRow.oppijaOid
        assert(oikeudet.map(_.oppijaOid).toSet == Set(oppijaOid), "Usean ja/tai väärien henkilöiden tietoja henkilöllä " + oppijaOid + ": " + oikeudet)
        Observable.just((oppijaOid, oikeudet.toList))
      case _ =>
        Observable.empty
    }
  }


  override def createOrUpdate(oppijaOid: PossiblyUnverifiedOppijaOid, opiskeluOikeus: KoskeenTallennettavaOpiskeluoikeus)(implicit user: KoskiUser): Either[HttpStatus, CreateOrUpdateResult] = {
    try {
      runDbSync(createOrUpdateAction(oppijaOid, opiskeluOikeus).transactionally)
    } catch {
      case e:SQLException if e.getSQLState == "23505" =>
        // 23505 = Unique constraint violation
        Left(KoskiErrorCategory.conflict.samanaikainenPäivitys())
    }
  }

  private def findByOppijaOidAction(oid: String)(implicit user: KoskiUser): dbio.DBIOAction[Seq[OpiskeluOikeusRow], NoStream, Read] = {
    findAction(OpiskeluOikeudetWithAccessCheck.filter(_.oppijaOid === oid))
  }

  private def findByIdentifierAction(identifier: OpiskeluOikeusIdentifier)(implicit user: KoskiUser): dbio.DBIOAction[Either[HttpStatus, Option[OpiskeluOikeusRow]], NoStream, Read] = identifier match{
    case PrimaryKey(id) => {
      findAction(OpiskeluOikeudetWithAccessCheck.filter(_.id === id)).map { rows =>
        rows.headOption match {
          case Some(oikeus) => Right(Some(oikeus))
          case None => Left(KoskiErrorCategory.notFound.opiskeluoikeuttaEiLöydyTaiEiOikeuksia("Opiskeluoikeutta " + id + " ei löydy tai käyttäjällä ei ole oikeutta sen katseluun"))
        }
      }
    }

    case OppijaOidJaLähdejärjestelmänId(oppijaOid, lähdejärjestelmäId) => {
      findUnique(oppijaOid, { row =>
        row.toOpiskeluOikeus.lähdejärjestelmänId == Some(lähdejärjestelmäId)
      })
    }

    case i:OppijaOidOrganisaatioJaTyyppi => {
      findUnique(i.oppijaOid, { row =>
        OppijaOidOrganisaatioJaTyyppi(i.oppijaOid, row.toOpiskeluOikeus.oppilaitos.oid, row.toOpiskeluOikeus.tyyppi.koodiarvo, row.toOpiskeluOikeus.lähdejärjestelmänId) == identifier
      })
    }
  }

  private def findUnique(oppijaOid: String, f: OpiskeluOikeusRow => Boolean)(implicit user: KoskiUser) = {
    findByOppijaOidAction(oppijaOid).map(_.filter(f).toList).map {
      case List(singleRow) => Right(Some(singleRow))
      case Nil => Right(None)
      case multipleRows => Left(KoskiErrorCategory.internalError(s"Löytyi enemmän kuin yksi rivi päivitettäväksi (${multipleRows.map(_.id)})"))
    }
  }

  private def findAction(query: Query[OpiskeluOikeusTable, OpiskeluOikeusRow, Seq])(implicit user: KoskiUser): dbio.DBIOAction[Seq[OpiskeluOikeusRow], NoStream, Read] = {
    query.result
  }

  private def createOrUpdateAction(oppijaOid: PossiblyUnverifiedOppijaOid, opiskeluOikeus: KoskeenTallennettavaOpiskeluoikeus)(implicit user: KoskiUser): dbio.DBIOAction[Either[HttpStatus, CreateOrUpdateResult], NoStream, Read with Write with Transactional] = {
    findByIdentifierAction(OpiskeluOikeusIdentifier(oppijaOid.oppijaOid, opiskeluOikeus)).flatMap { rows: Either[HttpStatus, Option[OpiskeluOikeusRow]] =>
      rows match {
        case Right(Some(vanhaOpiskeluOikeus)) =>
          updateAction(vanhaOpiskeluOikeus, opiskeluOikeus)
        case Right(None) =>
          oppijaOid.verifiedOid match {
            case Some(oid) => createAction(oid, opiskeluOikeus)
            case None => DBIO.successful(Left(KoskiErrorCategory.notFound.oppijaaEiLöydy("Oppijaa " + oppijaOid.oppijaOid + " ei löydy.")))
          }
        case Left(err) => DBIO.successful(Left(err))
      }
    }
  }

  private def createAction(oppijaOid: String, opiskeluOikeus: KoskeenTallennettavaOpiskeluoikeus)(implicit user: KoskiUser): dbio.DBIOAction[Either[HttpStatus, CreateOrUpdateResult], NoStream, Write] = {
    opiskeluOikeus.versionumero match {
      case Some(versio) if (versio != VERSIO_1) =>
        DBIO.successful(Left(KoskiErrorCategory.conflict.versionumero(s"Uudelle opiskeluoikeudelle annettu versionumero $versio")))
      case _ =>
        val tallennettavaOpiskeluOikeus = opiskeluOikeus.withIdAndVersion(id = None, versionumero = None)
        for {
          opiskeluoikeusId <- OpiskeluOikeudet.returning(OpiskeluOikeudet.map(_.id)) += new OpiskeluOikeusRow(oppijaOid, tallennettavaOpiskeluOikeus, VERSIO_1)
          diff = Json.toJValue(List(Map("op" -> "add", "path" -> "", "value" -> tallennettavaOpiskeluOikeus)))
          _ <- historyRepository.createAction(opiskeluoikeusId, VERSIO_1, user.oid, diff)
        } yield {
          Right(Created(opiskeluoikeusId, VERSIO_1, diff))
        }
    }
  }

  private def updateAction[A <: PäätasonSuoritus](oldRow: OpiskeluOikeusRow, uusiOpiskeluoikeus: KoskeenTallennettavaOpiskeluoikeus)(implicit user: KoskiUser): dbio.DBIOAction[Either[HttpStatus, CreateOrUpdateResult], NoStream, Write] = {
    val (id, versionumero) = (oldRow.id, oldRow.versionumero)

    uusiOpiskeluoikeus.versionumero match {
      case Some(requestedVersionumero) if (requestedVersionumero != versionumero) =>
        DBIO.successful(Left(KoskiErrorCategory.conflict.versionumero("Annettu versionumero " + requestedVersionumero + " <> " + versionumero)))
      case _ =>
        val vanhaOpiskeluoikeus = oldRow.toOpiskeluOikeus

        val täydennettyOpiskeluoikeus = OpiskeluoikeusChangeMigrator.kopioiValmiitSuorituksetUuteen(vanhaOpiskeluoikeus, uusiOpiskeluoikeus)

        val uusiData = Json.toJValue(täydennettyOpiskeluoikeus.withIdAndVersion(id = None, versionumero = None))
        val diff: JArray = Json.jsonDiff(oldRow.data, uusiData)
        diff.values.length match {
          case 0 =>
            DBIO.successful(Right(NotChanged(id, versionumero, diff)))
          case _ =>
            validateOpiskeluoikeusChange(vanhaOpiskeluoikeus, täydennettyOpiskeluoikeus) match {
              case HttpStatus.ok =>
                val nextVersionumero = versionumero + 1
                for {
                  rowsUpdated <- OpiskeluOikeudetWithAccessCheck.filter(_.id === id).map(row => (row.data, row.versionumero)).update((uusiData, nextVersionumero))
                  _ <- historyRepository.createAction(id, nextVersionumero, user.oid, diff)
                } yield {
                  rowsUpdated match {
                    case 1 => Right(Updated(id, nextVersionumero, diff))
                    case x: Int =>
                      throw new RuntimeException("Unexpected number of updated rows: " + x) // throw exception to cause rollback!
                  }
                }
              case nonOk => DBIO.successful(Left(nonOk))
            }
        }
    }
  }
}