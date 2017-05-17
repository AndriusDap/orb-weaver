package andriusdap.orbweaver.decompress

import java.lang.Long
import java.util.function.Consumer

import it.unimi.dsi.big.webgraph.BVGraph

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.reflect.io.File

object App {

  def l[T](message: String, f: () => T) = {
    val before = System.currentTimeMillis()

    print(s"$message ... ")

    val result = f.apply()
    val after = System.currentTimeMillis()
    println(f" ... ${(after - before) / 1000.0f}%2.2f s")

    result
  }

  case class AppParams(webgraph: Option[String] = None, seedFile: Option[String] = None, outFile: Option[String] = None, passes: Option[Int] = None)

  def seed(length: Int, seedFile: Option[String]): Array[Float] = {
    seedFile match {
      case Some(file) =>
        val values = Array.fill[Float](length)(0.0f)
        Source.fromFile(file).getLines().map(_.toInt).foreach(values(_) = 1.0f)
        values
      case None => Array.fill[Float](length)(1.0f)
    }
  }

  def output(pagerank: Array[Float], s: String): Unit = {
    File(s).writeAll(pagerank.zipWithIndex.map { case (rank, index) => s"$index $rank\n" }: _*)
  }

  def main(args: Array[String]): Unit = {
    val applicationConfig = args.sliding(2, 1).foldLeft[AppParams](AppParams()) {
      case (current, params) =>
        params.toList match {
          case "--webgraph" :: tail => current.copy(webgraph = tail.headOption)
          case "--seed" :: tail => current.copy(seedFile = tail.headOption)
          case "--out" :: tail => current.copy(outFile = tail.headOption)
          case _ => current
        }
    }

    applicationConfig match {
      case AppParams(Some(webgraph), optSeedFile, optOutFile, optPasses) =>
        val graph = BVGraph.load(webgraph)

        val transposedGraph = l("transpose", () => transpose(graph))
        val outDegree = l("out degree", () => outDegrees(graph))
        val seedRank = l("seed file", () => seed(transposedGraph.length, optSeedFile))

        val passes = optPasses.getOrElse(50)

        val pagerank = (1 to passes).foldLeft(seedRank) {
          case (currentRank, pass) =>

            val newValue = l(s"pass #$pass", () => singlePass(transposedGraph, outDegree, currentRank))
            println(s"diff is ${distance(currentRank, newValue)}")

            newValue
        }

        optOutFile.foreach(f => l("outputing", () => output(pagerank, f)))
      case _ => println("invalid params, please specify --seed, --webgraph, --out, --passes")
    }
  }

  def distance(current: Array[Float], previous: Array[Float]): Double = {
    var i = 0
    var sum = 0.0d
    while (i < current.length) {
      val diff = current(i) - previous(i)
      sum += diff * diff
      i += 1
    }

    Math.sqrt(sum)
  }

  def singlePass(transposedGraph: Array[Array[Int]], outDegrees: Array[Int], rank: Array[Float]): Array[Float] = {
    //transposedGraph.map(0.15f + 0.85f * _.map(link => rank(link) / outDegrees(link)).sum)

    val newRank = new Array[Float](transposedGraph.length)

    var i = 0
    while (i < transposedGraph.length) {
      var sum = 0.0f
      val links = transposedGraph(i)

      var j = 0
      while (j < links.length) {
        val current = links(j)
        sum += rank(current) / outDegrees(current)
        j += 1
      }

      newRank(i) = 0.15f + 0.85f * sum

      i += 1
    }

    newRank
  }

  def transpose(graph: BVGraph): Array[Array[Int]] = {
    require(graph.numNodes() < Int.MaxValue, s"Cannot handle more than maxint nodes, received ${graph.numNodes()}")

    val transposedGraph = Array.fill(graph.numNodes().toInt)(new ArrayBuffer[Int]())

    graph.nodeIterator(0).forEachRemaining(
      new Consumer[Long] {
        override def accept(t: Long): Unit = {
          val successors = graph.successorBigArray(t)

          var i = 0
          while (i < successors.length) {
            var j = 0

            val asInt = t.toInt
            while (j < successors(i).length) {
              val current = successors(i)(j).toInt
              transposedGraph(current).append(asInt)

              j += 1
            }
            i += 1
          }
        }
      }
    )
    transposedGraph.map(_.toArray)
  }

  def outDegrees(graph: BVGraph): Array[Int] = {
    val outDegrees = new Array[Int](graph.numNodes().toInt)
    graph.nodeIterator(0).forEachRemaining(
      new Consumer[Long] {
        override def accept(t: Long): Unit = {
          outDegrees(t.toInt) = graph.outdegree(t).toInt
        }
      }
    )

    outDegrees
  }
}
