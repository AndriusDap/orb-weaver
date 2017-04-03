package andriusdap.orbweaver.urlsplit

object WowpalWabbit {
  def main(args: Array[String]): Unit = {
    Iterator.continually(scala.io.StdIn.readLine()).takeWhile(n => n != null && n.nonEmpty).foreach {
      line =>
        val c = line.split("  ").toList
        val (url, label) = c match {
          case head :: tail => (UrlBuilder.build(head), tail.headOption)
          case _ => ???
        }

        val path = url.host.reverse.toList match {
          case tld :: secondLevelDomain :: tail => s"TLD$tld 2ND$secondLevelDomain ${tail.mkString(" ")}"
          case tld :: secondLevelDomain  => s"TLD$tld 2ND$secondLevelDomain"
          case List(tld) => s"TLD:$tld"
          case _ => ""
        }

        println(s"${label.getOrElse("")} |path $path |query ${url.query.mkString(" ")}|numerical dots:${url.dots}.0 specialSymbols:${url.specialSymbols}.0")
    }
  }
}
