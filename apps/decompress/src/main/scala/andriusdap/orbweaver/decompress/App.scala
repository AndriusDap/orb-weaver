package andriusdap.orbweaver.decompress

import it.unimi.dsi.big.webgraph.ArcListASCIIGraph

object App {
  def main(args: Array[String]): Unit = {
    val dir = args.sliding(2, 1).foldLeft[Option[String]](None) {
      case (current, params) =>
        params.toList match {
          case "--webgraph" :: tail => tail.headOption
          case _ => current
        }
    }.get

    ArcListASCIIGraph.main(Array(
      "-g", "BVGraph", dir, dir + ".edges"
    ))
  }
}
