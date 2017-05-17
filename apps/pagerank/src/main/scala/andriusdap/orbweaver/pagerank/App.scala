package andriusdap.orbweaver.pagerank

import org.apache.spark._
import org.apache.spark.graphx.{Graph, _}
import org.apache.spark.graphx.lib.PageRank
import org.apache.spark.rdd.RDD

object App {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
      .setAppName("Pagerank")
      .setSparkHome(System.getenv("SPARK_HOME"))
      .setJars(SparkContext.jarOfClass(this.getClass).toList)

    val sc = new SparkContext(conf)

    val edges: RDD[Edge[String]] =
      sc.textFile(args(0)).map { line =>
        val fields = line.split("\\s+")
        Edge(fields(0).toLong, fields(1).toLong, fields(0))
      }

    val seed = sc.textFile(args(1)).map { line =>
        line.toLong
    }.collect()

    val graph : Graph[Any, String] = Graph.fromEdges(edges, "defaultProperty")

    val ranked = PageRank.runParallelPersonalizedPageRank(
      graph,
      1,
      sources = seed
    )

    println(ranked.vertices.collect().take(100).mkString("\n"))
  }
}
