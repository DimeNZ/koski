package fi.oph.tor.toruser

import fi.oph.tor.servlet.HtmlServlet
import fi.vm.sade.security.ldap.DirectoryClient

class LogoutServlet(val userRepository: UserOrganisationsRepository, val directoryClient: DirectoryClient) extends HtmlServlet {
  get("/") {
    logger.info("Logged out")
    Option(request.getSession(false)).foreach(_.invalidate())
    redirectToLogin
  }
}
