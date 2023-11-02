import Utilz.CreateLogger

// The following class is used to parse the command-line arguments for the main function.
class ArgumentParser {

  val logger = CreateLogger(classOf[ArgumentParser])
  logger.info("ArgumentParser is ready to parse the arguments.")

  // Parse the command-line arguments and return them as an Option of a tuple.
  def parse(args: Array[String]): Option[(String, String, String, String, String, String, String, String, Int, String)] = {
    if (args.length != 10) {
      println("Usage: YourMainObject <originalGraphFileName> " +
        "<originalGraphPath> <perturbedGraphFileName> <perturbedGraphPath> " +
        "<Random Path Output Path> <Random File Path Access> " +
        "<Combined File Path> <Output SimRank With Traceability Links Path> <Iterations> <Loaded Original Nodes>")
      System.exit(("Invalid number of arguments. Exiting...").toInt)
    }

    logger.info("Arguments are parsed successfully.")

    // Extract individual arguments from the command-line arguments.
    val originalGraphFileName = args(0)
    val originalGraphPath = args(1)
    val perturbedGraphFileName = args(2)
    val perturbedGraphPath = args(3)
    val randomPathOutputPath = args(4)
    val randomFilePath = args(5)
    val combinedFilePath = args(6)
    val outputSimRankWithTracebilityLinksPath = args(7)
    val walks = args(8)
    val loadedNodes = args(9)

    logger.info("The files were loaded. Initializing the RandomWalker.")

    // Call the main function to run the random walk.
    RandomWalker.runRandomWalk(originalGraphFileName, originalGraphPath, perturbedGraphFileName, perturbedGraphPath, randomPathOutputPath, randomFilePath, combinedFilePath, outputSimRankWithTracebilityLinksPath, walks.toInt, loadedNodes)

    // Return the parsed arguments as a tuple wrapped in Some.
    Some((originalGraphFileName, originalGraphPath, perturbedGraphFileName, perturbedGraphPath, randomPathOutputPath, randomFilePath, combinedFilePath, outputSimRankWithTracebilityLinksPath, walks.toInt, loadedNodes))
  }
}



