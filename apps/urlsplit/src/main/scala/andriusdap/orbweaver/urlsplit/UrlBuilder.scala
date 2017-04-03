package andriusdap.orbweaver.urlsplit

import java.net.{URI, URLDecoder}

import scala.language.postfixOps


object UrlBuilder {

  def dropPrefix(s: String): String = {
    val banned = Vector("https://www.", "http://www.", "https://", "http://", "www.")

    banned.find(s startsWith).map(prefix => s.substring(prefix.length)).getOrElse(s)
  }

  case class URL(host: Vector[String], query: Vector[String], dots: Int, specialSymbols: Int)

  def split(s: String): URL = {
    val separators = Array('-', '.', '_', '~', ':', '/', '?', '#', '[', ']', '@', '!', '$', '&', ''', '(', ')', '*', '+', ',', ';', '=', ' ')
    val slashed = s.toLowerCase.split("/")
    val host = slashed.headOption.map(_.split(separators).toVector).getOrElse(Vector.empty)
    val path = slashed.tail.flatMap(param => param.split(separators)).toVector

    val dotCount = s.count(_ == '.')
    URL(host, path, dotCount, s.length - (host.map(_.length).sum + path.map(_.length).sum) - dotCount)
  }

  def decode(s: String): String = try {
    URLDecoder.decode(s)
  } catch {
    case _ => s
  }

  def build: (String) => URL = decode _ andThen dropPrefix andThen split
}
