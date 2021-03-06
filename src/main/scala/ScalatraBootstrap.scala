import javax.servlet.ServletContext

import fi.oph.koski.cache.{CacheServlet, JMXCacheManager, GlobalCacheManager}
import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.db._
import fi.oph.koski.documentation.SchemaDocumentationServlet
import fi.oph.koski.editor.EditorServlet
import fi.oph.koski.fixture.{FixtureServlet, Fixtures}
import fi.oph.koski.history.KoskiHistoryServlet
import fi.oph.koski.koodisto.KoodistoCreator
import fi.oph.koski.koski.{HealthCheckServlet, KoskiJsonSchemaValidator, OppijaServlet}
import fi.oph.koski.koskiuser._
import fi.oph.koski.log.Logging
import fi.oph.koski.oppilaitos.OppilaitosServlet
import fi.oph.koski.servlet.IndexServlet
import fi.oph.koski.suoritusote.SuoritusServlet
import fi.oph.koski.tiedonsiirto.TiedonsiirtoServlet
import fi.oph.koski.todistus.TodistusServlet
import fi.oph.koski.tutkinto.TutkintoServlet
import fi.oph.koski.util.Pools
import org.scalatra._

import scala.concurrent.Future

class ScalatraBootstrap extends LifeCycle with Logging with GlobalExecutionContext {
  override def init(context: ServletContext) {

    Future {
      // Parallel warm-up: org.reflections.Reflections takes a while to scan
      KoskiJsonSchemaValidator.henkilöSchema
    }

    Pools.init
    val application = Option(context.getAttribute("koski.application").asInstanceOf[KoskiApplication]).getOrElse(KoskiApplication.apply)

    if (application.config.getBoolean("koodisto.create")) {
      tryCatch("Koodistojen luonti") { KoodistoCreator.createKoodistotFromMockData(application.config) }
    }

    if (application.config.getBoolean("käyttöoikeusryhmät.create")) {
      tryCatch("Käyttöoikeusryhmien luonti/päivitys") { KäyttöoikeusRyhmätCreator.luoKäyttöoikeusRyhmät(application.config) }
    }

    context.mount(new OppijaServlet(application), "/api/oppija")
    context.mount(new EditorServlet(application), "/api/editor")
    context.mount(new HealthCheckServlet(application), "/api/healthcheck")
    context.mount(new KoskiHistoryServlet(application), "/api/opiskeluoikeus/historia")
    context.mount(new TiedonsiirtoServlet(application), "/api/tiedonsiirrot")
    context.mount(new UserServlet(application), "/user")
    context.mount(new CasServlet(application), "/cas")
    context.mount(new LogoutServlet(application), "/user/logout")
    context.mount(new OppilaitosServlet(application), "/api/oppilaitos")
    context.mount(new TutkintoServlet(application.tutkintoRepository), "/api/tutkinto")
    context.mount(new SchemaDocumentationServlet(application.koodistoPalvelu), "/documentation")
    context.mount(new TodistusServlet(application), "/todistus")
    context.mount(new SuoritusServlet(application), "/opintosuoritusote")
    context.mount(new IndexServlet(application), "/")
    context.mount(new CacheServlet(application), "/cache")

    if (Fixtures.shouldUseFixtures(application.config)) {
      context.mount(new FixtureServlet(application), "/fixtures")
      application.fixtureCreator.resetFixtures
    }
  }

  override def destroy(context: ServletContext) = {
  }

  private def tryCatch(thing: String)(task: => Unit): Unit = {
    try {
      task
    } catch {
      case e: Exception => logger.error(e)(thing + " epäonnistui: " + e.getMessage)
    }
  }
}