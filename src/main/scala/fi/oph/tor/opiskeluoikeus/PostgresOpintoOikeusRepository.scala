package fi.oph.tor.opiskeluoikeus

import fi.oph.tor.db.PostgresDriverWithJsonSupport.api._
import fi.oph.tor.db.Tables._
import fi.oph.tor.db.TorDatabase.DB
import fi.oph.tor.db._
import fi.oph.tor.http.HttpStatus
import fi.oph.tor.schema.{FullHenkilö, OpiskeluOikeus}
import fi.oph.tor.user.UserContext

class PostgresOpiskeluOikeusRepository(db: DB) extends OpiskeluOikeusRepository with Futures with GlobalExecutionContext {
  // Note: this is a naive implementation. All filtering should be moved to query-level instead of in-memory-level

  override def filterOppijat(oppijat: Seq[FullHenkilö])(implicit userContext: UserContext) = {
    val all = findAllRows
    oppijat.filter { oppija => all.exists(opiskeluOikeus => opiskeluOikeus.oppijaOid == oppija.oid)}
  }

  override def findByOppijaOid(oid: String)(implicit userContext: UserContext): Seq[OpiskeluOikeus] = {
    find(OpiskeluOikeudet.filter(_.oppijaOid === oid))
  }

  override def create(oppijaOid: String, opiskeluOikeus: OpiskeluOikeus) = {
    Right(await(db.run(OpiskeluOikeudet.returning(OpiskeluOikeudet.map(_.id)) += new OpiskeluOikeusRow(oppijaOid, opiskeluOikeus))))
  }

  private def findAll(implicit userContext: UserContext): Seq[OpiskeluOikeus] = {
    findAllRows.map(_.toOpiskeluOikeus)
  }

  override def find(identifier: OpiskeluOikeusIdentifier)(implicit userContext: UserContext) = identifier match{
    case PrimaryKey(id) => find(OpiskeluOikeudet.filter(_.id === id)).headOption
    case IdentifyingSetOfFields(oppijaOid, oppilaitosOrganisaatio, ePerusteetDiaarinumero) => {
      findByOppijaOid(oppijaOid).find({
        new IdentifyingSetOfFields(oppijaOid, _) == identifier
      })
    }
  }

  override def update(oppijaOid: String, opiskeluOikeus: OpiskeluOikeus) = {
    // TODO: always overriding existing data can not be the eventual update strategy
    val rowsUpdated: Int = await(db.run(OpiskeluOikeudet.filter(_.id === opiskeluOikeus.id.get).map(_.data).update(new OpiskeluOikeusRow(oppijaOid, opiskeluOikeus).data)))
    rowsUpdated match {
      case 1 => HttpStatus.ok
      case x => HttpStatus.internalError("Unexpected number of updated rows: " + x)
    }
  }

  private def findAllRows(implicit userContext: UserContext): Seq[OpiskeluOikeusRow] = {
    findRows(OpiskeluOikeudet)
  }

  private def find(filter: Query[OpiskeluOikeusTable, OpiskeluOikeusRow, Seq])(implicit userContext: UserContext): Seq[OpiskeluOikeus] = {
    findRows(filter).map(_.toOpiskeluOikeus)
  }

  private def findRows(filter: PostgresDriverWithJsonSupport.api.Query[OpiskeluOikeusTable, OpiskeluOikeusRow, Seq])(implicit userContext: UserContext): Seq[OpiskeluOikeusRow] = {
    await(db.run(filter.result)).filter(withAccess)
  }

  private def withAccess(oikeus: OpiskeluOikeusRow)(implicit userContext: UserContext) = {
    userContext.hasReadAccess(oikeus.toOpiskeluOikeus.oppilaitos)
  }
}