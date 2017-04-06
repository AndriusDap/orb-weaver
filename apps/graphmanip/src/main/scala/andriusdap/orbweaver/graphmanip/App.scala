package andriusdap.orbweaver.graphmanip

import java.io.FileInputStream
import java.nio.ByteBuffer
import java.text.NumberFormat
import java.util.zip.GZIPInputStream

import it.unimi.dsi.big.webgraph.BVGraph
import it.unimi.dsi.logging.ProgressLogger
import org.rocksdb.{WriteBatch, _}
import com.timgroup.iterata.ParIterator.Implicits._

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

  def buildDb(db: String, index: String): Unit = {
    RocksDB.loadLibrary()



    val options = new Options()
      .setCompressionType(CompressionType.BZLIB2_COMPRESSION)
      .setCreateIfMissing(true)
      .prepareForBulkLoad()

    val dba = RocksDB.open(options, db)

    var i = 0

    val startTime = System.currentTimeMillis()
    val writeOptions = new WriteOptions()
    val count = 5000
    val parser = NumberFormat.getNumberInstance
    readFile(index).getLines().grouped(count).map {
      batch =>
        val wb = new WriteBatch()
        batch.foreach {
          line =>
            val pair = line.split(" ")
            try {

              val id = parser.parse(pair.last).longValue
              val url = pair.dropRight(1).mkString("  ")

              val hash = longHash(url)

              wb.put(toBytes(hash), toBytes(id))

            } catch {
              case idgaf: Exception => None
            }
        }
        wb
    }.zipWithIndex.foreach {
      case (wb, id) =>
        println(s"$startTime/${System.currentTimeMillis()}: ${(id * count).toDouble / 1700000000.0} done.")
        dba.write(writeOptions, wb)
    }


    dba.close()
  }

  @inline
  def toBytes(hash: Long): Array[Byte] = {
    val hashBuffer = ByteBuffer.allocate(java.lang.Long.BYTES)
    hashBuffer.asLongBuffer().put(hash)
    hashBuffer.array()
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
