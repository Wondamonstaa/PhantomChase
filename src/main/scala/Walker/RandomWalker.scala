package Walker

import DataManipulation.{Mixer, DataConverter, ArgumentParser}

import NetGraphAlgebraDefs.{NetGraph, NodeObject}
import SimRankAlgorithm.SimRank
import com.typesafe.config.ConfigFactory
import org.apache.hadoop.io.{DoubleWritable, Text}
import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.slf4j.Logger

import java.net.InetAddress
import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.util.Random

object RandomWalker {

  class RandomWalker() extends Serializable {}

  val logger: Logger = org.slf4j.LoggerFactory.getLogger("RandomWalker")
  val config = ConfigFactory.load()

  //The following helper function is used to get the random node on the graph
  def getRandomNode(netGraph: NetGraph, random: Random, currentNode: NodeObject): NodeObject = {

    // Get the neighbors of the current node via state machine successor function
    val neighbors = netGraph.sm.successors(currentNode).asScala.toSeq

    // Return a random neighbor if there are any, otherwise return the current node
    neighbors.headOption.getOrElse(currentNode)
  }

  // Function to calculate the adjacency matrix
  def adjacencyMatrix(netGraph: NetGraph): Array[Array[Float]] = {
    // Retrieve the nodes from the netGraph
    val nodes: Array[NodeObject] = netGraph.sm.nodes().asScala.toArray
    // Create a 2D array for the adjacency matrix
    val matrix: Array[Array[Float]] = Array.ofDim[Float](nodes.length, nodes.length)
    // Iterate through the nodes to populate the adjacency matrix
    nodes.indices.foreach(i =>
      nodes.indices.foreach(j =>
        if (netGraph.sm.hasEdgeConnecting(nodes(i), nodes(j))) {
          // If there is an edge between nodes, set the matrix value to the edge cost
          matrix(i)(j) = netGraph.sm.edgeValue(nodes(i), nodes(j)).get.cost.toFloat
        } else {
          // If there is no edge, set the matrix value to positive infinity
          matrix(i)(j) = Float.PositiveInfinity
        }
      )
    )
    // Return the generated adjacency matrix
    matrix
  }


  /**
   * Helper function to explore a path in the graph starting from the current node.
   *
   * @param netGraph          The graph object.
   * @param adjacencyMatrix   The adjacency matrix representing graph connections.
   * @param currentNode       The current node to start the exploration.
   * @param path              The list of nodes in the current path (initially contains only the currentNode).
   * @param randomPathDetails ListBuffer to store details of the random path.
   * @return The list of nodes visited during the exploration.
   */
  @tailrec
  def explorePath(
                   netGraph: NetGraph,
                   adjacencyMatrix: Array[Array[Float]],
                   currentNode: NodeObject,
                   path: List[NodeObject],
                   randomPathDetails: ListBuffer[String]
                 ): List[NodeObject] = {

    // Find the index of the current node in the graph's nodes.
    val currentNodeIndex = netGraph.sm.nodes().asScala.toSeq.indexOf(currentNode)

    // Find neighbors with finite edge costs.
    val neighbors = adjacencyMatrix(currentNodeIndex)
      .zipWithIndex
      .filter { case (cost, index) => cost < Float.PositiveInfinity && index != currentNodeIndex }
      .map { case (_, index) => netGraph.sm.nodes().asScala.toSeq(index) }

    if (neighbors.isEmpty) {
      // If no neighbors left, extract and append details of each visited node to randomPathDetails.
      val pattern = "\\((.*?)\\)".r
      randomPathDetails ++= path.map { node =>
        val extracted = pattern.findFirstMatchIn(node.toString).map(_.group(1)).getOrElse("")
        extracted
      }
      path
    } else {
      // Randomly select the next node from the neighbors and continue the exploration.
      val nextNode = neighbors(Random.nextInt(neighbors.length))
      SimRank.maxNodesInWalk = Math.max(SimRank.maxNodesInWalk, path.length + 1)
      SimRank.nodesInWalks.addOne(nextNode.id)
      explorePath(netGraph, adjacencyMatrix, nextNode, path :+ nextNode, randomPathDetails)
    }
  }


  /**
   *  The following function performs the random walk algorithm on the perturbed graph by randomly generating the inital node to start.
   *  The additional getRandomConnectedNode function is used to randomly select a node from the neighbors of the current node.
   *  The randomWalk function is called from the main function in the SimRank object.
   *  Finally, the path walked is stored in the randomPathDetails ListBuffer.
   *
   * @param netGraph             The graph object.
   * @param adjacencyMatrix      The adjacency matrix representing graph connections.
   * @param iterations           The number of random walk iterations to perform.
   * @param sparkContext         The SparkContext for distributed processing.
   * @param randomPathOutputPath The path to save the random path details.
   * @return The count of successful attacks.
   */
  def randomWalk(
                  netGraph: NetGraph,
                  adjacencyMatrix: Array[Array[Float]],
                  iterations: Int,
                  sparkContext: SparkContext,
                  randomPathOutputPath: String
                ): Int = {
    val random = new Random
    val successfulAttacks = sparkContext.parallelize(Seq(0))
    val randomPathDetails = ListBuffer[String]()
    SimRank.minNodesInWalk = Int.MaxValue

    // Perform random walk for the specified number of iterations.
    for (_ <- 0 until iterations) {
      val cur = netGraph.initState
      var currentNode = getRandomNode(netGraph, random, cur)
      var path = List(currentNode)

      // Explore the path starting from the current node and update the path details.
      val finalPath: List[NodeObject] = explorePath(netGraph, adjacencyMatrix, currentNode, path, randomPathDetails)
      currentNode = finalPath.last

      // Check if the current node contains valuable data and increment successful attacks if true.
      if (currentNode.valuableData) {
        successfulAttacks.foreachPartition { _ =>
          successfulAttacks.map { count =>
            count + 1
          }.collect()
        }
      }
    }

    // Convert random path details to an RDD, coalesce it to a single partition, and save it to the specified path.
    val randomPathOutputRDD: RDD[String] = sparkContext.parallelize(randomPathDetails.toSeq)
    val coalescedRDD = randomPathOutputRDD.coalesce(1)
    coalescedRDD.saveAsTextFile(randomPathOutputPath)

    // Return the count of successful attacks.
    successfulAttacks.reduce(_ + _)
  }


  /**
   * The following helper function serves as a driver of the core functionality of the RandomWalk algorithm.
   *
   * @param originalGraphFileName                 The filename of the original graph.
   * @param originalGraphPath                     The path of the original graph file.
   * @param perturbedGraphFileName                The filename of the perturbed graph.
   * @param perturbedGraphPath                    The path of the perturbed graph file.
   * @param randomPathOutputPath                  The path to save the random path details.
   * @param randomFilePath                        The path for the random file.
   * @param combinedFilePath                      The path for the combined CSV file.
   * @param outputSimRankWithTracebilityLinksPath The path to save the SimRank with Traceability Links results.
   * @param iterations                            The number of random walk iterations.
   */
  //@main
  def runRandomWalk(
                     originalGraphFileName: String,
                     originalGraphPath: String,
                     perturbedGraphFileName: String,
                     perturbedGraphPath: String,
                     randomPathOutputPath: String,
                     randomFilePath: String,
                     combinedFilePath: String,
                     outputSimRankWithTracebilityLinksPath: String,
                     iterations: Int,
                     loadedOriginalNodes: String
                   ): Unit = {

    logger.info("Loading in original graph ngs and perturbed graph ngs files using NetGraph.load function:")

    // Get the host name and IP address for logging.
    val hostName = InetAddress.getLocalHost.getHostName
    val ipAddr = InetAddress.getLocalHost.getHostAddress

    // Load the original and perturbed graphs.
    val originalGraph: Option[NetGraph] = NetGraph.load(originalGraphFileName, originalGraphPath)
    val perturbedGraph: Option[NetGraph] = NetGraph.load(perturbedGraphFileName, perturbedGraphPath)

    // Initialize the Spark session.
    val spark = SparkSession.builder()
      .master("local[*]")
      .appName("RandomWalker")
      .getOrCreate()

    // Check if the graphs have been loaded successfully.
    val netPerturbedGraph: NetGraph = perturbedGraph.getOrElse {
      logger.error("Failed to load the perturbed graph.")
      System.exit(1)
      throw new RuntimeException("Failed to load the perturbed graph.")
    }

    val netOriginalGraph: NetGraph = originalGraph.getOrElse {
      logger.error("Failed to load the original graph.")
      System.exit(1)
      throw new RuntimeException("Failed to load the original graph.")
    }

    logger.info("The graphs have been loaded successfully.")

    // Obtain the adjacency matrix of the perturbed counterparts.
    logger.info("Obtaining the adjacency matrix of the perturbed counterparts")
    val adj = adjacencyMatrix(netPerturbedGraph)
    val successfulAttacks = randomWalk(netPerturbedGraph, adj, iterations, spark.sparkContext, randomPathOutputPath)

    // Create a combination of the original and perturbed graph => creates the combined CSV.
    Mixer.exec(originalGraphFileName, originalGraphPath, randomPathOutputPath, randomFilePath, combinedFilePath, loadedOriginalNodes)

    // After performing the random walk and creating the combined CSV file, read the combined file.
    val combinedData: RDD[String] = spark.sparkContext.textFile(combinedFilePath)

    // Calculate SimRankWithTraceabilityLinksEdge for each line.
    val simRankWithTraceabilityLinksEdgeResults: RDD[String] = combinedData.map(line => {
      // The SimRank will accept a line of the combined CSV file and return the SimRankWithTraceabilityLinksEdge.
      val simRankResult = SimRank.calculateSimRank(line)
      SimRank.calculateSimRankWithTracebilityLinks(new Text(), List(new DoubleWritable(simRankResult)).asJava)
    })

    // Write SimRankWithTraceabilityLinksEdge results to a text file.
    simRankWithTraceabilityLinksEdgeResults.saveAsTextFile(outputSimRankWithTracebilityLinksPath)

    // Stop the Spark session.
    spark.stop()
    logger.info(s"Stopped")
  }


  def main(args: Array[String]): Unit = {}
}


