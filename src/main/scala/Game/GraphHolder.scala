package org

import NetGraphAlgebraDefs.NetGraph
import NetGraphAlgebraDefs.NetGraph.logger
import Utilz.NGSConstants.obtainConfigModule
import com.typesafe.config.{Config, ConfigFactory}


//The following object is used to load and store the original and perturbed graphs.
object GraphHolder {

  // Loads the configuration file
  val CONFIGENTRYNAME: String = "NGSimulator"
  val config: Config = ConfigFactory.load()
  val globalConfig: Config = obtainConfigModule(config, CONFIGENTRYNAME)

  //Required parameters for the simulation
  val originalGraphFileName: String = globalConfig.getString("originalGraph")
  val originalGraphPath: String = globalConfig.getString("originalGraphPath")
  val perturbedGraphFileName: String = globalConfig.getString("perturbedGraphFileName")
  val perturbedGraphPath: String = globalConfig.getString("perturbedGraphPath")
  val yamlPath: String = globalConfig.getString("yamlOutput")
  val sparkPath: String = globalConfig.getString("sparkOutput")

  // Load the original and perturbed graphs.
  val originalGraph: Option[NetGraph] = NetGraph.load(originalGraphFileName, originalGraphPath)
  val perturbedGraph: Option[NetGraph] = NetGraph.load(perturbedGraphFileName, perturbedGraphPath)

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

  def main(args: Array[String]): Unit = {
  }
}




