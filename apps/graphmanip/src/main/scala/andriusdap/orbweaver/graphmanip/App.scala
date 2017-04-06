package andriusdap.orbweaver.graphmanip

import java.io.FileInputStream
import java.nio.ByteBuffer
import java.text.NumberFormat
import java.util.zip.GZIPInputStream

import it.unimi.dsi.big.webgraph.BVGraph
import it.unimi.dsi.logging.ProgressLogger
import org.rocksdb.{WriteBatch, _}

import scala.io.BufferedSource
import scala.util.Random




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

  def buildDb(db: String, index: String): Unit = {
    RocksDB.loadLibrary()

    val options = new Options()
      .setWalDir(db)
      .setCompressionType(CompressionType.BZLIB2_COMPRESSION)
      .setCreateIfMissing(true)
      .setIncreaseParallelism(4)
      .prepareForBulkLoad()

    val dba = RocksDB.open(options, db)

    val startTime = System.currentTimeMillis()
    val writeOptions = new WriteOptions()
    writeOptions.setSync(true)
    val count = 10000
    val parser = NumberFormat.getNumberInstance

    readFile(index).getLines().grouped(count).map {
      batch =>
        batch.par.flatMap {
          line =>
            val pair = line.split(" ")
            try {
              val id = parser.parse(pair.last).longValue
              val url = pair.dropRight(1).mkString("  ")
              val hash = longHash(url)
              Some(toBytes(hash), toBytes(id))
            } catch {
              case idgaf: Exception => None
            }
        }.foldLeft(new WriteBatch()) {
          case (wb, (hash, id)) =>
            wb.put(hash, id)
            wb
        }
    }.zipWithIndex.foldLeft(dba){
      case (dba, (wb, id)) =>
        eta(startTime, id * count / 1700000000.0)
        dba.write(writeOptions, wb)
        wb.close()
        dba
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
