package andriusdap.orbweaver.graphmanip

import java.nio.ByteBuffer

import org.rocksdb._

class WriteDb(filename: String) {
  import Database._

  private val db: RocksDB = {
    RocksDB.loadLibrary()

    RocksDB.open(new Options()
      .setWalDir(filename)
      .setCompressionType(CompressionType.BZLIB2_COMPRESSION)
      .setCreateIfMissing(true)
      .setIncreaseParallelism(4)
      .prepareForBulkLoad(), filename)
  }

  def write(values: Seq[(Long, Long)]): Unit = {
    val options = new WriteOptions()
    val writeBatch = new WriteBatch()

    values.map { case (k, v) => toBytes(k) -> toBytes(v)}.foreach { case (k, v) => writeBatch.put(k, v) }

    db.write(options, writeBatch)

    writeBatch.close()
    options.close()
  }

  def close(): Unit = {
    db.compactRange()
    db.close()
  }

}

class ReadDb(filename: String) {
  import Database._

  private val db: RocksDB = {
    RocksDB.loadLibrary()

    RocksDB.openReadOnly(filename)
  }

  def read(keys: Seq[Long]): Seq[Long] = {
    keys.map(toBytes).map(db.get).map(toLong)
  }
}

object Database {
  @inline
  def toBytes(hash: Long): Array[Byte] = {
    val hashBuffer = ByteBuffer.allocate(java.lang.Long.BYTES)
    hashBuffer.asLongBuffer().put(hash)
    hashBuffer.array()
  }

  @inline
  def toLong(bytes: Array[Byte]): Long = {
    ByteBuffer.wrap(bytes).getLong
  }

  def main(args: Array[String]): Unit = {
    val max = 1000000l
    val wdb = new WriteDb("/media/ssd/tmp")
    (1l until max).grouped(5000).foreach {
      g =>
        wdb.write(
          g.map(c => (c, c))
      )
    }

    wdb.close()

    val rdb = new ReadDb("/media/ssd/tmp")
    val read = rdb.read(Seq(1l, max))

    println(s"read values, first is ${read.head} last is ${read.last}")

  }

}
