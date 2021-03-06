package fi.oph.koski.util

import scala.io.{BufferedSource, Source}
import scala.reflect.io.File

object Files {
  def exists(filename: String) = asSource(filename).isDefined
  def asByteArray(filename: String): Option[Array[Byte]] = asSource(filename).map(_.takeWhile(_ != -1).map(_.toByte).toArray)
  def asString(filename: String): Option[String] = asSource(filename).map(_.mkString)
  def resourceAsString(resourcename: String): Option[String] = loadResource(resourcename).map(_.mkString)
  def asSource(filename: String) = {
    loadFile(filename)
  }

  private def loadFile(filename: String): Option[BufferedSource] = {
    File(filename).exists match {
      case true => Some(Source.fromFile(filename))
      case false => None
    }
  }

  private def loadResource(resourcename: String): Option[BufferedSource] =
    Option(getClass.getResourceAsStream(resourcename)).map(Source.fromInputStream)
}

trait FileOps {
  def exists(filename: String) = asSource(filename).isDefined
  def asByteArray(filename: String): Option[Array[Byte]] = asSource(filename).map(_.takeWhile(_ != -1).map(_.toByte).toArray)
  def asString(filename: String): Option[String] = asSource(filename).map(_.mkString)
  def asSource(filename: String): Option[Source]
}