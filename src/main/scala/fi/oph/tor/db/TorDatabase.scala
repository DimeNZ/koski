package fi.oph.tor.db

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory._
import fi.oph.tor.log.Logging
import fi.oph.tor.util.{Pools, PortChecker}
import org.flywaydb.core.Flyway
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._

import scala.sys.process._

object TorDatabase {
  type DB = PostgresDriver.backend.DatabaseDef

  implicit class TorDatabaseConfig(c: Config) {
    val host: String = c.getString("db.host")
    val port: Int = c.getInt("db.port")
    val dbName: String = c.getString("db.name")

    val config = c.getConfig("db")
      .withValue("url", fromAnyRef("jdbc:postgresql://"+host+":"+port+"/"+dbName))
      .withValue("numThreads", fromAnyRef(Pools.dbThreads))

    val password: String = config.getString("password")
    val user: String = config.getString("user")
    val url: String = config.getString("url")
    def isLocal = host == "localhost"
    def isRemote = !isLocal
    def toSlickDatabase = Database.forConfig("", config)
  }
}

class TorDatabase(val config: Config) extends Logging {
  import TorDatabase._

  val serverProcess = startLocalDatabaseServerIfNotRunning

  if (!config.isRemote) {
    createDatabase
    createUser
  }

  val db: DB = config.toSlickDatabase

  migrateSchema

  private def startLocalDatabaseServerIfNotRunning: Option[PostgresRunner] = {
    if (!isDbRunning) {
      Some(startEmbedded)
    } else {
      None
    }
  }

  private def isDbRunning = {
    if (config.isRemote) {
      logger.info("Using remote PostgreSql database at " + config.host + ":" + config.port)
      true
    } else if (!PortChecker.isFreeLocalPort(config.port)) {
      logger.info("PostgreSql already running on port " + config.port)
      true
    } else {
      false
    }
  }

  private def startEmbedded: PostgresRunner = {
    new PostgresRunner("postgresql/data", "postgresql/postgresql.conf", config.port).start
  }

  private def createDatabase = {
    val dbName = config.dbName
    val port = config.port
    s"createdb -p $port -T template0 -E UTF-8 $dbName" !;
  }

  private def createUser = {
    val user = config.user
    s"createuser -s $user -w"!
  }

  private def migrateSchema = {
    try {
      val flyway = new Flyway
      flyway.setDataSource(config.url, config.user, config.password)
      flyway.setSchemas(config.user)
      flyway.setValidateOnMigrate(false)
      if (System.getProperty("tor.db.clean", "false").equals("true")) {
        flyway.clean
      }
      flyway.migrate
    } catch {
      case e: Exception => logger.warn("Migration failure", e)
    }
  }
}




