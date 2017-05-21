transposedGraph.map(
    (1 - dampening) + dampening * _.map(
      link => rank(link) / outDegrees(link)
    ).sum
  )