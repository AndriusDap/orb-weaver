package andriusdap.orbweaver.core

import andriusdap.orbweaver.executor.App.FeatureSet
import andriusdap.orbweaver.executor.{App, UrlBuilder}

import scala.io.Source
import scala.language.postfixOps
import scala.sys.process._

object Classifier {
  lazy val maliciousRanks = loadPageranks(
    "/media/ssd/temp_dir/malicious_pagerank.txt")

  lazy val benignRanks = loadPageranks(
    "/media/ssd/temp_dir/benign_pagerank.txt")

  val model = "/media/ssd/vw/model.vw"

  lazy val minMalicious = maliciousRanks.values.min
  lazy val minBenign = benignRanks.values.min

  def loadPageranks(source: String): Map[String, Float] = {
    Source.fromFile(source).getLines().map{c =>
      val s = c.split(" ")
      s(0) -> s(1).toFloat
    }.toMap
  }

  def classify(source: String): String = {
    val host = App.strip(source)
    val url = UrlBuilder.build(source)
    val malicious = maliciousRanks.getOrElse(host, minMalicious)
    val benign = benignRanks.getOrElse(host, minBenign)

    val sample = App.toSample(FeatureSet(
      url.host,
      url.query,
      url.dots,
      url.specialSymbols,
      malicious,
      benign,
      App.Benign
    ))

    val result = Process("echo", Seq(sample)) #| Process(
      "/media/ssd/vw2/test.sh"
    ) lineStream_!

    val head = result.head
    head
  }
}
