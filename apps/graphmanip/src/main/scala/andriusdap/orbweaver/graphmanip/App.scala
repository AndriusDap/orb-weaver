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

    db.close()
  }

  def readFile(index: String): BufferedSource = {
    scala.io.Source.fromInputStream(new GZIPInputStream(new FileInputStream(index)))
  }

  case class InputParams(
    dbOutput: Option[String],
    indexInput: Option[String]
  )

  def main(args: Array[String]): Unit = {
    val params = args.sliding(2, 1).foldLeft(InputParams(None, None)) {
      case (params, window) =>
        window.toList match {
          case "--db" :: tail => params.copy(dbOutput = tail.headOption)
          case "--index" :: tail => params.copy(indexInput = tail.headOption)
          case _ => params
        }
    }

    params match {
      case InputParams(Some(db), Some(index)) =>
        buildDb(db, index)
      case _ => println("requred params: --db <db output file> --index <index input file, gzipped>")
    }
  }
}
