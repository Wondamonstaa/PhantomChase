package DataManipulation

import NetGraphAlgebraDefs.NetGraph.logger
import NetGraphAlgebraDefs.{NetGraph, NodeObject}
import Walker.RandomWalker
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import java.io._
import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`
import scala.io.Source

object Mixer {

  /**
   * Write nodes to a CSV file.
   *
   * @param outputPath The file path to the output CSV file.
   * @param nodes      A set of NodeObject representing the nodes to be written to the CSV.
   */
  def writeNodesToCSV(outputPath: String, nodes: java.util.Set[NodeObject]): Unit = {
    val writer = new PrintWriter(outputPath)
    nodes.foreach { node =>
      val properties = node.toString.split("\\(")(1).split("\\)")(0)
      writer.println(properties)
    }

    writer.close()
  }


  /**
   * Combine and write lines from perturbed file with lines from original file to a CSV file.
   *
   * @param perturbedFilePath The file path to the perturbed data file.
   * @param originalFilePath  The file path to the original data file.
   * @param combinedFilePath  The file path to the output CSV file.
   */
  def combineAndWriteToCSV(perturbedFilePath: String, originalFilePath: String, combinedFilePath: String): Unit = {
    // Read the contents of the perturbed and original files
    val perturbedLines = Source.fromFile(perturbedFilePath).getLines().toList.drop(1) // Skip header
    val originalLines = Source.fromFile(originalFilePath).getLines().toList.drop(1) // Skip header

    // Create a CSV file to write the combined lines
    val csvFile = new File(combinedFilePath)
    val csvWriter = new PrintWriter(new FileWriter(csvFile))

    // Generate combined lines using flatMap and map
    val combinedLines = originalLines.flatMap { originalLine =>
      perturbedLines.map(perturbedLine => s"$originalLine, $perturbedLine")
    }

    // Write combined lines to CSV
    combinedLines.foreach(combinedLine => csvWriter.println(combinedLine))

    // Close the CSV file writer
    csvWriter.close()
  }

  /**
   * Combine and write lines from perturbed file with lines from original file to a CSV file using Spark.
   *
   * @param spark             The SparkSession to use for DataFrame operations.
   * @param perturbedFilePath The file path to the perturbed data file.
   * @param originalFilePath  The file path to the original data file.
   * @param combinedFilePath  The file path to the output CSV file.
   */
  def combineAndWriteToCSV(spark: SparkSession, perturbedFilePath: String, originalFilePath: String, combinedFilePath: String): Unit = {
    // Read the contents of the perturbed and original files as DataFrames
    val perturbedDF: DataFrame = spark.read.text(perturbedFilePath)
    val originalDF: DataFrame = spark.read.text(originalFilePath)

    // Extract lines as strings
    val perturbedLines = perturbedDF.select(col("value").as("perturbed"))
    val originalLines = originalDF.select(col("value").as("original"))

    // Cross join to create the Cartesian product
    val combinedDF = originalLines.crossJoin(perturbedLines)

    // Write the combined DataFrame to a CSV file
    combinedDF.write.csv(combinedFilePath)
  }


  //@main
  /**
   * Apache Spark is a distributed computing framework designed for big data processing and analytics.
   * Spark operates in a distributed cluster environment, allowing it to efficiently process large datasets in parallel.
   * To do so, Spark relies on a cluster manager (Mesos, YARN) to allocate and manage resources across the cluster.
   * @param spark
   * @param originalGraphFileName
   * @param originalGraphPath
   * @param randomPathOutputPath
   * @param randomFilePath
   * @param combinedFilePath
   * @param loadedOriginalNodes
   */
  def exec(spark: SparkSession, originalGraphFileName: String, originalGraphPath: String, randomPathOutputPath: String, randomFilePath: String, combinedFilePath: String, loadedOriginalNodes: String): Unit = {

    // Here we load in the Original Ngs Graph
    logger.info("Loading in Original Graph ngs file using NetGraph.load function:")
    val originalGraph: Option[NetGraph] = NetGraph.load(originalGraphFileName, originalGraphPath)
    logger.info("Original Graph was successfully loaded")

    // Getting information of Original Graph
    logger.info("Gathering Information of the Original Graph")
    val netOriginalGraph: NetGraph = originalGraph.get // getiting original graph info
    logger.info("Information successfully extracted for Original Graph")

    // Gathering the Nodes in original graph
    logger.info("Storing Nodes in a list for Original Graph")
    val originalGraphNodes: java.util.Set[NodeObject] = netOriginalGraph.sm.nodes()
    logger.info("Original Node were successfully stored within a list")

    // Creating Csv file to store all of the original nodes in a csv file
    // This will be used in creating Cartesian Product of original X perturbed
    logger.info("Storing Original Graph Nodes in a CSV file")
    writeNodesToCSV(loadedOriginalNodes, originalGraphNodes)
    logger.info("Original Graph Nodes were successfully stored in a Csv File")

    // Creating a Cartesian product of Perturbed X Original
    // Perturbed Nodes are those generated via Random Walk using Spark
    // Original Nodes are were directly stored in .txt
    logger.info("Creating Csv File to store each Perturbed Node X Original Nodes in a CSV File")
    combineAndWriteToCSV(randomFilePath, loadedOriginalNodes, combinedFilePath)
    //combineAndWriteToCSV(spark, randomFilePath, loadedOriginalNodes, combinedFilePath)
    logger.info("Combined Csv of Perturbed X Original Graphs was Successfully created")
  }

  def main(args: Array[String]): Unit = {}
}