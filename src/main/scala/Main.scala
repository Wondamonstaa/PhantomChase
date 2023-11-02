import Utilz.NGSConstants.obtainConfigModule
import com.lsc.Main.logger
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.graphx._


object Main {

  //Abstraction for graph representation
  abstract class Graph[VD, ED] {
    val vertices: VertexRDD[VD]
    val edges: EdgeRDD[ED]
  }

  class VertexProperty()
  case class UserProperty(val name: String) extends VertexProperty
  case class ProductProperty(val name: String, val price: Double) extends VertexProperty

  // The graph might then have the type:
  var graph: Graph[VertexProperty, String] = null


  def main(args: Array[String]): Unit = {

    /*val config = ConfigFactory.load()
    val logger = org.slf4j.LoggerFactory.getLogger("Main")
    val hostName = InetAddress.getLocalHost.getHostName
    val ipAddr = InetAddress.getLocalHost.getHostAddress
    logger.info(s"Hostname: $hostName")
    logger.info(s"IP Address: $ipAddr")

      // Create SparkSession object
      val spark = SparkSession.builder()
        .master("local[1]")
        .appName("SparkByExamples.com")
        .getOrCreate();

      // Access spark context
      println("Spark App Name : " + spark.sparkContext.appName)

      val sc = spark.sparkContext*/

    val CONFIGENTRYNAME: String = "NGSimulator"
    val config: Config = ConfigFactory.load()
    val globalConfig: Config = obtainConfigModule(config, CONFIGENTRYNAME)

    val originalGraphFileName = globalConfig.getString("originalGraphFileName")
    val originalGraphPath = globalConfig.getString("originalGraphPath")
    val perturbedGraphFileName = globalConfig.getString("perturbedGraphFileName")
    val perturbedGraphPath = globalConfig.getString("perturbedGraphPath")
    val randomPathOutputPath = globalConfig.getString("randomPathOutputPath")
    val randomFilePath = globalConfig.getString("randomFilePath")
    val combinedFilePath = globalConfig.getString("combinedFilePath")
    val outputSimRankWithTracebilityLinksPath = globalConfig.getString("outputSimRankWithTracebilityLinksPath")
    val iterations = globalConfig.getString("iterations")
    val loadedOriginalNodes = globalConfig.getString("loadedOriginalNodes")

    //The following array contains the arguments that are passed to the ArgumentParser class
    val argsArray = Array(
      originalGraphFileName,
      originalGraphPath,
      perturbedGraphFileName,
      perturbedGraphPath,
      randomPathOutputPath,
      randomFilePath,
      combinedFilePath,
      outputSimRankWithTracebilityLinksPath,
      iterations,
      loadedOriginalNodes
    )

    //An object of ArgumentParser class is created => the "entry" point of the written code
    val argumentParser = new ArgumentParser()
    val parsedArguments = argumentParser.parse(argsArray)

    //Check if the args are valid
    parsedArguments match {
      case Some((originalGraphFileName, originalGraphPath,
      perturbedGraphFileName, perturbedGraphPath,
      randomPathOutputPath, randomFilePath,
      combinedFilePath, outputSimRankWithTracebilityLinksPath,
      iterations, loadedOriginalNodes)) =>

        logger.info(s"Original Graph File Name: $originalGraphFileName")
        logger.info(s"Original Graph Path: $originalGraphPath")
        logger.info(s"Perturbed Graph File Name: $perturbedGraphFileName")
        logger.info(s"Perturbed Graph Path: $perturbedGraphPath")
        logger.info(s"Random Path Output Path: $randomPathOutputPath")
        logger.info(s"Random File Path: $randomFilePath")
        logger.info(s"Combined File Path: $combinedFilePath")
        logger.info(s"Output SimRank With Tracebility Links Path: $outputSimRankWithTracebilityLinksPath")
        logger.info(s"Iterations: $iterations")
        logger.info(s"Loaded Original Nodes: $loadedOriginalNodes")

      case None =>
        logger.info("Invalid arguments provided.")
    }
  }
}