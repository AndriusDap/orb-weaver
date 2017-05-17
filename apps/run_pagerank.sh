#!/bin/zsh
sbt package && /opt/spark/bin/spark-submit  --class "andriusdap.orbweaver.pagerank.App"  pagerank/target/scala-2.11/pagerank_2.11-0.0.1-SNAPSHOT.jar ~/workspace/orb-weaver/apps/pagerank/src/test/resources/edges ~/workspace/orb-weaver/apps/pagerank/src/test/resources/seed ~/workspace/orb-weaver/apps/pagerank/src/test/resources/ranked
