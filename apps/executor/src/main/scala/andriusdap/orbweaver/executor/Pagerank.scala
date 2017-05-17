package andriusdap.orbweaver.executor

import java.lang.Long
import java.util.function.Consumer

import it.unimi.dsi.big.webgraph.BVGraph

import scala.collection.mutable.ArrayBuffer
import scala.io.{Codec, Source}
import scala.reflect.io.File

class Pagerank(webgraph: String) {

  val graph = BVGraph.load(webgraph)
  val transposedGraph = transpose(graph)
  val outDegree = outDegrees(graph)

  def seed(length: Int, seedFile: File, seedValue: Float): Array[Float] = {
    val values = Array.fill[Float](length)(0.0f)
    Source.fromFile(seedFile.jfile).getLines().map(_.toInt).foreach(values(_) = seedValue)
    values
  }

  def output(pagerank: Array[Float], s: String): Unit = {
    File(s).writeAll(pagerank.zipWithIndex.map { case (rank, index) => s"$index $rank\n" }: _*)
  }

  case class PagerankResult(pagerank: Array[Float], convergence: Seq[Double])

  def pagerank(seedFile: File, passes: Int, dampening: Float, error: Double, seedValue: Float): PagerankResult = {
    val seedRank = seed(transposedGraph.length, seedFile, seedValue)

    val (convergence, pagerank) = (1 to passes).foldLeft(Seq[Double]() -> seedRank) {
      case ((diffs, currentRank), pass) =>
        if(!diffs.lastOption.exists(_ < error)) {
          val newValue = singlePass(transposedGraph, outDegree, currentRank, dampening)
          (diffs :+ distance(currentRank, newValue)) -> newValue
        } else {
          (diffs, currentRank)
        }
    }
    PagerankResult(pagerank, convergence)
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

  def singlePass(transposedGraph: Array[Array[Int]], outDegrees: Array[Int], rank: Array[Float], dampening: Float = 0.85f): Array[Float] = {
    //transposedGraph.map(0.15f + 0.85f * _.map(link => rank(link) / outDegrees(link)).sum)

    val spring = 1 - dampening
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

      newRank(i) = spring + dampening * sum

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