package andriusdap.orbweaver.executor

import scala.language.postfixOps

object UrlBuilder {

  private def dropPrefix(s: String): String = {
    val banned =
      Vector("https://www.", "http://www.", "https://", "http://", "www.")

    banned
      .find(s startsWith)
      .map(prefix => s.substring(prefix.length))
      .getOrElse(s)
  }

  case class URL(host: Vector[String],
                 query: Vector[String],
                 dots: Int,
                 specialSymbols: Int)

  private def split(s: String): URL = {
    val separators = Array('-', '.', '_', '~', ':', '/', '?', '#', '[', ']',
      '@', '!', '$', '&', ''', '(', ')', '*', '+', ',', ';', '=', ' ')
    val slashed = s.toLowerCase.split("/")
    val host = slashed.headOption
      .map(_.split(separators).toVector)
      .getOrElse(Vector.empty)
    val path = slashed.tail.flatMap(param => param.split(separators)).toVector

    val dotCount = s.count(_ == '.')
    URL(
      host,
      path,
      dotCount,
      s.length - (host.map(_.length).sum + path.map(_.length).sum) - dotCount)
  }

  def build(s: String): URL =
    split(dropPrefix(s))
}
