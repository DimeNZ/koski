package fi.oph.tor.henkilo

import java.time.{DateTimeException, LocalDate}

import fi.oph.tor.http.HttpStatus.badRequest

import scala.util.matching.Regex

object Hetu {

  val checkChars = List('0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F','H','J','K','L','M','N','P','R','S','T','U','V','W','X','Y')
  val hetuRegex: Regex = "^(0[1-9]|1[0-9]|2[0-9]|3[0-1])(0[1-9]|1[0-2])([0-9][0-9])(A|-|\\+)([0-9]{3})([0-9A-Y])$".r

  def validate(hetu: String) = {

    def validFormat(hetu: String) = {
      hetuRegex.findFirstIn(hetu) match {
        case Some(_) => Right(hetu)
        case None => Left(badRequest("Virheellinen muoto hetulla: " + hetu))
      }
    }

    def validDate(hetu: String) = {
      val century = hetu.lift(6) match {
        case Some('+') => Some(1800)
        case Some('-') => Some(1900)
        case Some('A') => Some(2000)
        case _  => None
      }
      try {
        century.flatMap { century =>
          val birthday: LocalDate = LocalDate.of(century + hetu.slice(4, 6).toInt, hetu.slice(2, 4).toInt, hetu.slice(0, 2).toInt)
          if (birthday.isBefore(LocalDate.now)) Some(hetu) else None
        } match {
          case Some(_) => Right(hetu)
          case None => Left(badRequest("Syntymäpäivä hetussa: " + hetu + " on tulevaisuudessa"))
        }
      } catch {
        case ex: DateTimeException => Left(badRequest("Virheellinen syntymäpäivä hetulla: " + hetu))
      }
    }

    def validCheckChar(hetu: String) = {
      val checkChar = checkChars(Math.round(((hetu.slice(0,6) + hetu.slice(7,10)).toInt / 31.0 % 1) * 31).toInt)
      if (checkChar == hetu.last) Right(hetu) else Left(badRequest("Virheellinen tarkistusmerkki hetussa: " + hetu))
    }

    validFormat(hetu).right.flatMap(validDate(_).right.flatMap(validCheckChar))
  }
}