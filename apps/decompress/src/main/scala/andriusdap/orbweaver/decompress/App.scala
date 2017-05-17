package andriusdap.orbweaver.decompress

import java.lang.Long
import java.util.function.Consumer

import it.unimi.dsi.big.webgraph.{ArcListASCIIGraph, BVGraph}

object App {
  def main(args: Array[String]): Unit = {
    val dir = args.sliding(2, 1).foldLeft[Option[String]](None) {
      case (current, params) =>
        params.toList match {
          case "--webgraph" :: tail => tail.headOption
          case _ => current
        }
    }.get

    val graph = BVGraph.load(dir)

    require(graph.numNodes() < Int.MaxValue, s"Cannot handle more than maxint nodes, received ${graph.numNodes()}")

    var scores = Array.fill[Float](graph.numNodes().toInt)(1.0f)

    var newScores = new Array[Float](graph.numNodes().toInt)

    val dampening = 0.85f
    val oneMinusDampening = 1 - dampening

    val before = System.currentTimeMillis()
    println(s"starting first iteration ${System.currentTimeMillis()}")
    graph.nodeIterator(0).forEachRemaining (
      new Consumer[Long] {
        override def accept(t: Long): Unit = {
          val successors = graph.successorBigArray(t)

          var sum = 0.0f

          var i = 0
          while(i < successors.length) {
            var j = 0

            while(j < successors(i).length) {
              val currentl = successors(i)(j)
              val current = currentl.toInt
              sum += scores(current)/graph.outdegree(currentl)
              j += 1
            }

            i += 1
          }

          scores(t.toInt) = oneMinusDampening + dampening * sum
        }
      }
    )
    println(scores.take(100).mkString("\n"))
    println(s"done with iteration in ${System.currentTimeMillis() - before}ms")
  }
}
