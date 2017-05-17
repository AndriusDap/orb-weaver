package andriusdap.orbweaver.executor

import scala.io.{Codec, Source}
import scala.reflect.io.File
import scala.util.Random

object App {
  Random.setSeed(5l)

  val dir = "/media/ssd/final_data/"

  val tmpDir = "/media/ssd/temp_dir/"

  val hostgraph = dir + "hostgraph"
  val blacklist = dir + "blacklist.txt"
  val whitelist = dir + "decoded_whitelist.txt"
  val hostIndex = dir + "index.host"

  def l[T](message: String, f: () => T) = {
    val before = System.currentTimeMillis()

    print(s"$message ... ")

    val result = f.apply()
    val after = System.currentTimeMillis()
    println(f" ... ${(after - before) / 1000.0f}%2.2f s")

    result
  }

  sealed trait Label {
    def print = {
      this match {
        case Benign => "Benign"
        case Malicious => "Malicious"
      }
    }
  }

  object Label {
    def fromString: String => Label = {
      case "Benign" => Benign
      case "Malicious" => Malicious
      case _ => ???
    }
  }

  case object Malicious extends Label

  case object Benign extends Label

  case class Feature(url: String, label: Label)

  object Feature {
    def parse(line: String): Feature = {
      val fields = line.split(" ")
      Feature(fields.dropRight(1).mkString(" "), Label.fromString(fields.last))
    }
  }

  def main(args: Array[String]): Unit = {
    val (test, train, validate) = l("split files", () => splitFile(blacklist, whitelist))
    val (malicious, benign) = l("building seeds", () => getSeeds(train))
    val (maliciousPagerankFile, benignPagerankFile, maliciousPagerankConvergenceFile, benignPagerankConvergenceFile) = l("building pageranks", () => buildPageranks(malicious, benign))
  }

  def buildPageranks(malicious: File, benign: File): (File, File, File, File) = {
    val maliciousPagerankFile = File(tmpDir + "malicious_pagerank.txt")
    val benignPagerankFile = File(tmpDir + "benign_pagerank.txt")

    val maliciousPagerankConvergenceFile = File(tmpDir + "malicious_pagerank_convergence.txt")
    val benignPagerankConvergenceFile = File(tmpDir + "benign_pagerank_convergence.txt")

    if (Seq(maliciousPagerankFile, benignPagerankFile, maliciousPagerankConvergenceFile, benignPagerankConvergenceFile).exists(!_.exists)) {

      val maliciousPagerankFileBuffer = maliciousPagerankFile.bufferedWriter()
      val benignPagerankFileBuffer = benignPagerankFile.bufferedWriter()
      val maliciousPagerankConvergenceFileBuffer = maliciousPagerankConvergenceFile.bufferedWriter()
      val benignPagerankConvergenceFileBuffer = benignPagerankConvergenceFile.bufferedWriter()

      val pagerankAlgo = l("initializing pagerank", () => new Pagerank(hostgraph))
      val maliciousPagerank = l(s"malicious pagerank", () => pagerankAlgo.pagerank(malicious, 50, 0.7f, 0.1))
      val benignPagerank = l(s"benign pagerank", () => pagerankAlgo.pagerank(benign, 50, 0.75f, 0.1))


      val hosts = Source
        .fromFile(hostIndex)
        .getLines()
        .map {
          line =>
            line.split("\t")(0)
        }.toSeq

      maliciousPagerank.pagerank.zip(hosts).foreach { case (rank, host) =>
        maliciousPagerankFileBuffer.write(s"$host $rank\n")
      }

      benignPagerank.pagerank.zip(hosts).foreach { case (rank, host) =>
        benignPagerankFileBuffer.write(s"$host $rank\n")
      }

      maliciousPagerank.convergence.foreach(
        convergence =>
          maliciousPagerankConvergenceFileBuffer.write(s"$convergence\n")
      )

      benignPagerank.convergence.foreach(
        convergence =>
          benignPagerankConvergenceFileBuffer.write(s"$convergence\n")
      )

      maliciousPagerankFileBuffer.close()
      benignPagerankFileBuffer.close()
      maliciousPagerankConvergenceFileBuffer.close()
      benignPagerankConvergenceFileBuffer.close()
    }
    (maliciousPagerankFile, benignPagerankFile, maliciousPagerankConvergenceFile, benignPagerankConvergenceFile)
  }

  def getSeeds(train: File): (File, File) = {
    val malicious = File(tmpDir + "maliciousSeed.txt")
    val benign = File(tmpDir + "benignSeed.txt")
    if (Seq(malicious, benign).exists(!_.exists)) {
      val hosts = Source
        .fromFile(hostIndex)
        .getLines()
        .map {
          line =>
            val pair = line.split("\t")
            (pair(0), pair(1).toInt)
        }.toMap

      val trainItem = train.lines().map(Feature.parse).map {
        case Feature(url, label) =>
          Feature(strip(url), label)
      }
      val maliciousBuffer = malicious.bufferedWriter()
      val benignBuffer = benign.bufferedWriter()

      trainItem.flatMap {
        item =>
          hosts.get(item.url).map(_ -> item.label)
      }.foreach {
        case (id, label) =>
          val file = label match {
            case Benign => benignBuffer
            case Malicious => maliciousBuffer
          }

          file.write(s"$id\n")
      }
      benignBuffer.close()
      maliciousBuffer.close()
    }

    (malicious, benign)
  }

  def strip(url: String): String = {
    val prefixes = Seq(
      "https://www.",
      "http://www.",
      "https://",
      "http://"
    )

    val prefixLength = prefixes.find(url.startsWith).map(_.length).getOrElse(0)
    val withoutPrefix = url.drop(prefixLength)

    val suffixFrom = withoutPrefix.indexOf("/")

    val clean = if (suffixFrom == -1) {
      withoutPrefix
    } else {
      withoutPrefix.slice(0, suffixFrom)
    }
    clean
  }

  def splitFile(blacklist: String, whitelist: String): (File, File, File) = {
    val test = File(tmpDir + "test.txt")
    val train = File(tmpDir + "train.txt")
    val validate = File(tmpDir + "validate.txt")

    if (Seq(train, test, validate).exists(!_.exists)) {
      val blacklistLines = Source.fromFile(blacklist).getLines().map(s => Feature(s, Malicious)).toVector
      val whitelistLines = Source.fromFile(whitelist, "ISO-8859-1").getLines().map(s => Feature(s, Benign)).toVector

      val testBuffer = test.bufferedWriter()
      val trainBuffer = train.bufferedWriter()
      val validateBuffer = validate.bufferedWriter()

      Random.shuffle(blacklistLines ++ whitelistLines).foreach { line =>
        val target = Random.nextFloat() match {
          case a if a < 0.7 => trainBuffer
          case a if a < (0.7 + 0.25) => testBuffer
          case a => validateBuffer
        }

        target.write(s"${line.url} ${line.label.print}\n")
      }

      testBuffer.close()
      trainBuffer.close()
      validateBuffer.close()
    }

    (test, train, validate)
  }
}