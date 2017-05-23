package andriusdap.orbweaver.controllers

import com.google.inject.Singleton
import play.api.Logger
import java.io.File

import play.api.mvc._
import andriusdap.orbweaver.executor.App
import andriusdap.orbweaver.executor.UrlBuilder
import andriusdap.orbweaver.executor.App.FeatureSet

import sys.process._
import scala.io.{Codec, Source}
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

@Singleton
class Clasifier {
  lazy val maliciousRanks = loadPageranks("/media/ssd/temp_dir/malicious_pagerank.txt")
  lazy val benignRanks = loadPageranks("/media/ssd/temp_dir/benign_pagerank.txt")
  val model = "/media/ssd/vw/model.vw"

  lazy val minMalicious = maliciousRanks.values.min
  lazy val minBenign = benignRanks.values.min

  def loadPageranks(source: String): Map[String, Float] = {
    Source.fromFile(source).getLines().map{c =>
      val s = c.split(" ")
      s(0) -> s(1).toFloat
    }.toMap
  }

  def classify(source: String) = Action {
    request =>
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
//s"vw -t -i $model -p /dev/stdout --quiet"
      val result = Process("echo", Seq(sample)) #| Process("/media/ssd/vw/test.sh") lineStream_!

      Results.Ok(result.head)
  }
}