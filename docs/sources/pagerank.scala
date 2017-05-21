
def pagerank(transposedGraph: Array[Array[Int]],
  outDegrees: Array[Int],
  rank: Array[Float],
  dampening: Float = 0.85f): Array[Float] = {

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