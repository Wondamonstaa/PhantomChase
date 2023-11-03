package RDDSuite

import NetGraphAlgebraDefs.NetGraph.logger
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.graphx.{Edge, Graph, GraphLoader, GraphXUtils, TripletFields, VertexId}

object EdgeRDDSuite {
  /** Manages a local `sc` {@link SparkContext} variable, correctly stopping it after each test. */
  trait LocalSparkContext {
    /** Runs `f` on a new SparkContext and ensures that it is stopped afterwards.
     *
     * @param f A function that takes a SparkContext and returns a result of type T.
     * @tparam T The type of the result returned by the function.
     * @return The result returned by the function.
     */
    def withSpark[T](f: SparkContext => T): T = {
      val conf = new SparkConf()
      GraphXUtils.registerKryoClasses(conf)
      val sc = new SparkContext("local", "test", conf)
      try {
        f(sc)
      }
      finally {
        sc.stop()
        sc.parallelize(Seq.empty[Int]).count() // force stop
      }
    }
  }

  /** Define a method to perform random walks on a graph.
   *
   * @param sc       The SparkContext used for parallelization.
   * @param graph    The Graph representing the graph data.
   * @param numWalks The number of random walks to perform.
   */
  def tripleWalk(sc: SparkContext, graph: Graph[String, Double], numWalks: Int): Unit = {
    val vertices = graph.vertices

    // Initialize random walkers at each vertex.
    val walkerInitialRDD = vertices.map { case (vertexId, _) => (vertexId, Seq(vertexId)) }

    // Perform a fixed number of random walks (numWalks).
    val walksRDD = (0 until numWalks).foldLeft(walkerInitialRDD) { (walkers, _) =>
      val triplets = graph.triplets
        .flatMap { triplet =>
          if (math.random < 0.85) {
            Seq((triplet.srcId, triplet.dstId)) // Continue walking to the destination vertex
          } else {
            Seq((triplet.srcId, triplet.srcId)) // Return to the source vertex with a probability
          }
        }

      walkers.join(triplets)
        .map { case (vertexId, (walk, nextVertex)) =>
          (nextVertex, walk :+ nextVertex)
        }
    }

    // Collect and print random walks.
    val allWalks = walksRDD.map { case (_, walk) => walk.mkString(" -> ") }
    allWalks.collect().foreach(println)

    // Store in the output directory
    allWalks.saveAsTextFile("/Users/wondamonsta/Desktop/UIC/2023/Fall2023/CS441CloudProject2/Spark_Output/tripleWalk.txt")
  }


  def main(args: Array[String]): Unit = {

    logger.info("Initializing the Spark configurations for the TripleWalk")
    // Initialize Spark configurations
    val conf = new SparkConf().setAppName("TripleTravel").setMaster("local")
    val sc = new SparkContext(conf)

    // Generate random synthetic graph data for testing
    val vertices = sc.parallelize((1 to 10).map(vertexId => (vertexId.toLong, s"Node $vertexId")))
    val edges = sc.parallelize((1 to 20).map(_ => Edge((math.random * 10).toLong, (math.random * 10).toLong, math.random)))

    val graph = Graph(vertices, edges)

    // Specify the number of random walks to perform.
    val numWalks = 10

    // Perform random walks on the graph.
    tripleWalk(sc, graph, numWalks)

    // Stop the SparkContext.
    sc.stop()
  }
}
