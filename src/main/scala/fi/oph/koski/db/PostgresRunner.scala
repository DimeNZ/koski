package fi.oph.koski.db

import java.io.File
import java.nio.file.Files

import fi.oph.koski.log.Logging

class PostgresRunner(dataDirName: String, configFile: String, port: Integer) extends Logging {

  import sys.process._

  lazy val dataPath = new File(dataDirName).toPath

  private var serverProcess: Option[Process] = None

  private def ensureDataDirExists = {
    if (!dataDirExists) {
      createDataDir
    } else {
      logger.info("Data directory exists")
    }
  }

  private def createDataDir = {
    logger.info("Initializing data directory")
    Files.createDirectory(dataPath)
    s"chmod 0700 $dataDirName" !;
    s"initdb -D $dataDirName" !;
  }

  def jdbcUrl: String = s"jdbc:postgresql://localhost:$port/$dataDirName"

  def start = {
    if (!serverProcess.isDefined) {
      ensureDataDirExists
      logger.info("Starting server on port " + port)
      serverProcess = Some(("postgres --config_file=" + configFile + " -D " + dataDirName + " -p " + port).run)
      Thread.sleep(1000)
      sys.addShutdownHook {
        stop
      }
    }
    this
  }

  def stop() = {
    serverProcess.foreach(_.destroy())
    serverProcess = None
  }

  private def dataDirExists = Files.exists(dataPath)
}
