package andriusdap.orbweaver.graphmanip

import java.io.FileInputStream
import java.text.NumberFormat
import java.util.zip.GZIPInputStream

import scala.io.BufferedSource

object App {
  def longHash(s: String): Long = {
    //Java.hashCode from String.
    val length = s.length
    var hash: Long = 0
    var i = 0
    while(i < length) {
      hash = 31 * hash + s.charAt(i)
      i += 1
    }
    hash
  }

  def eta(start: Long, progress: Double): Unit = {
    val elapsed = (System.currentTimeMillis() - start) / 60000.0
    val total = elapsed/progress
    println(f"ETA: ${total - elapsed}%2.2f min ($progress%2.4f%%)")
  }

  def buildReverseDb(database: String, index: String): Unit = {
    val count = 5000
    val parser = NumberFormat.getNumberInstance

    val db = new WriteDb(database)

    val startTime = System.currentTimeMillis()
    readFile(index).getLines().grouped(count).map {
      batch =>
        batch.par.flatMap {
          line =>
            val pair = line.split("\t")
            try {
              val id = parser.parse(pair.last).longValue
              val url = pair.dropRight(1).mkString("  ")
              Some(id -> url)
            } catch {
              case idgaf: Exception => None
            }
        }
    }.zipWithIndex.foreach{
      case (wb, id) =>
        eta(startTime, id * count / 1700000000.0)
        db.writeStrings(wb.seq)
    }

    println("starting with compacting. May take some time.")
    db.close()
  }

  def buildDb(database: String, index: String): Unit = {
    val count = 5000
    val parser = NumberFormat.getNumberInstance

    val db = new WriteDb(database)

    val startTime = System.currentTimeMillis()
    readFile(index).getLines().grouped(count).map {
      batch =>
        batch.par.flatMap {
          line =>
            val pair = line.split("\t")
            try {
              val id = parser.parse(pair.last).longValue
              val url = pair.dropRight(1).mkString("  ")
              val hash = longHash(url)
              Some(hash -> id)
            } catch {
              case idgaf: Exception => None
            }
        }
    }.zipWithIndex.foreach{
      case (wb, id) =>
        eta(startTime, id * count / 1700000000.0)
        db.write(wb.seq)
    }


    println("starting with compacting. May take some time.")
    db.close()
  }

  def readFile(index: String): BufferedSource = {
    scala.io.Source.fromInputStream(new GZIPInputStream(new FileInputStream(index)))
  }

  case class InputParams(
    dbOutput: Option[String],
    indexInput: Option[String],
    mode: Option[Mode]
  )

  sealed trait Mode
  case object UrlToIndex extends Mode
  case object IndexToUrl extends Mode

  def main(args: Array[String]): Unit = {
    val params = args.sliding(2, 1).foldLeft(InputParams(None, None, None)) {
      case (params, window) =>
        window.toList match {
          case "--db" :: tail => params.copy(dbOutput = tail.headOption)
          case "--index" :: tail => params.copy(indexInput = tail.headOption)
          case "--id_to_url" :: Nil => params.copy(mode = Some(IndexToUrl))
          case "--url_to_id" :: Nil => params.copy(mode = Some(UrlToIndex))
          case "--id_to_url" :: tail => params.copy(mode = Some(IndexToUrl))
          case "--url_to_id" :: tail => params.copy(mode = Some(UrlToIndex))
          case _ => params
        }
    }

    params match {
      case InputParams(Some(db), Some(index), Some(UrlToIndex)) =>
        buildDb(db, index)
      case InputParams(Some(db), Some(index), Some(IndexToUrl)) =>
        buildReverseDb(db, index)
      case _ => println("requred params: --db <db output file> --index <index input file, gzipped> --id_to_url or --url_to_id to select mode")
    }
  }
}
