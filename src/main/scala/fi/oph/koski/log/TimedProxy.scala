package fi.oph.koski.log

import fi.oph.koski.util.{Proxy, Timing}
import org.log4s._
import scala.reflect.ClassTag

object TimedProxy {
  def apply[S <: AnyRef](service: S, thresholdMs: Int = 5)(implicit tag: ClassTag[S]) = {
    val logger = LoggerWithContext(getLogger(service.getClass), None)

    Proxy.createProxy[S](service, { invocation =>
      Timing.timed(invocation.toString, thresholdMs, logger) {invocation.invoke}
    })
  }
}