import DataManipulation.ArgumentParser
import Utilz.NGSConstants.obtainConfigModule
import com.lsc.Main.logger
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.graphx._
import Game.GameLogic._


object Main {

  def main(args: Array[String]): Unit = {

    //Loading the configurations from the application.conf file
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

    //The following array contains the arguments that are passed to the DataManipulation.ArgumentParser class => DONE
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

    //An object of DataManipulation.ArgumentParser class is created => the "entry" point of the written code => DONE
    val argumentParser = new ArgumentParser()
    val parsedArguments = argumentParser.parse(argsArray)

    //Check if the args are valid => DONE
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