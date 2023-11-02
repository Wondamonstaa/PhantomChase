package SimRankAlgorithm

import Utilz.CreateLogger
import breeze.numerics.abs
import org.apache.hadoop.io._
import org.slf4j.Logger
import java.text.DecimalFormat
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Random

object SimRank {

  //The following case class is used to store the properties of the nodes
  case class NodeProperties(
                             children: Double,
                             props: Double,
                             currentDepth: Double,
                             propValueRange: Double,
                             maxDepth: Double,
                             maxBranchingFactor: Double,
                             maxProperties: Double,
                             storedValue: Double,
                             valuableData: Boolean,
                           )

//The following case class is used to store the properties of the edges
  case class EdgeProperties(
                             sourceNode: String, // The source node identifier of the edge
                             targetNode: String, // The target node identifier of the edge
                             weight: Double, // The weight or similarity measure of the edge
                           )

  // Define variables for min, max, median, and sum of nodes in walks
  var minNodesInWalk: Int = Int.MinValue
  var maxNodesInWalk = 0
  var sumNodesInWalk = 0
  var numChangedProperties = 0
  var nodeNumber: Option[Int] = None

  //Tracebility links variables
  var ATL: Double = 0.0 // Number of correctly accepted TLs
  var CTL: Double = 0.0 // Number of correct TLs mistakenly discarded
  var DTL: Double = 0.0 // Number of correctly discarded TLs
  var WTL: Double = 0.0 // Number of wrong TLs accepted

  //Precision + recall variables
  var TP: Double = 0.0 //True positive
  var FP: Double = 0.0 //False positive
  var FN: Double = 0.0 //False negative
  val TN: Double = 0.0 //True negative

  import scala.collection.mutable.ArrayBuffer

  // Define arrays to collect the number of nodes in each walk
  val nodesInWalks: ArrayBuffer[Double] = ArrayBuffer[Double]()
  // Create a mutable set to store encountered node numbers
  var processedNodeNumbers = scala.collection.mutable.Set[Integer]()

  val logger: Logger = CreateLogger(classOf[SimRank.type])
  logger.info(s"Starting SimRank - loading configs")

  def main(args: Array[String]): Unit = {}

  //Allows to compare properties with a threshold
  def comparator(original: Double, perturbed: Double, thresholdPercentage: Double): Boolean = {
    val threshold = original * (thresholdPercentage / 100.0)
    Math.abs(original - perturbed) <= threshold
  }

  //Compares boolean with a threshold
  def compareBooleans(original: Boolean, perturbed: Boolean): Boolean = {
    // Compare two Boolean values for equality
    original == perturbed && original == true && perturbed == true
  }

  /*Function to calculate F1-score:
  It is calculated from the precision and recall of the test,
  where the precision is the number of true positive results divided by the number of all positive results,
  including those not identified correctly, and the recall is the number of true positive results divided by the number of all samples that should have been identified as positive.
   */
  def calculateF1Score(tp: Double, fp: Double, fn: Double): Double = {
    val precision = tp / (tp + fp)
    val recall = tp / (tp + fn)

    if (precision + recall == 0) {
      //The case where precision + recall is zero
      0.0
    }
    else {
      2.0 * (precision * recall) / (precision + recall)
    }
  }

  // Function to calculate  => where specificity is the percentage of true negatives
  def calculateSpecificity(tn: Double, fp: Double): Double = {
    tn / (tn + fp)
  }

  def calculateEdgeSimRank(csvLine: String): Double = {

    logger.info(s"Calculating Edge SimRank for $csvLine")
    println(csvLine)

    logger.info("Splitting CSV line")
    val fields = csvLine.split(",")

    var score: Double = 0.0

    // Properties of the original edge
    val originalEdgeProps = EdgeProperties(
      sourceNode = fields(0).trim,
      targetNode = fields(1).trim,
      weight = fields(2).trim.toDouble, // Assuming the weight is the similarity measure for edges
    )

    // Properties of the perturbed edge
    val perturbedEdgeProps = EdgeProperties(
      sourceNode = fields(3).trim,
      targetNode = fields(4).trim,
      weight = fields(5).trim.toDouble, // Assuming the weight is the similarity measure for edges
    )

    // Check if properties have changed (you can customize these thresholds)
    val propertiesChanged = List(
      !comparator(originalEdgeProps.weight, perturbedEdgeProps.weight, 5.75), // 5.75% threshold for weight
    )

    // Define the rewards for each property match
    val rewards = List(
      0.9, // Weight match
    )

    // Calculate the total number of changed properties
    val numChangedProperties = propertiesChanged.count(_ == true)

    logger.info(s"Number of changed properties: $numChangedProperties")
    val totalReward = propertiesChanged.zip(rewards).foldLeft(0.0) { case (acc, (propertyChanged, reward)) =>
      if (!propertyChanged) acc + reward else acc
    }

    // Define the threshold for similarity
    val similarityThreshold = 0.9

    // Calculate the total similarity score based on the total reward and the threshold
    val similarityScore = totalReward / rewards.sum

    // Adjust the similarity score based on the threshold
    val adjustedScore = if (totalReward > 0) {
      val normalizedScore = similarityScore / similarityThreshold
      normalizedScore
    }
    else {
      0.0
    }

    val df = new DecimalFormat("#.##")

    // Return the adjusted score
    df.format(adjustedScore).toDouble
  }

  def calculateSimRankWithTracebilityLinksEdge(key: Text, values: java.lang.Iterable[DoubleWritable]): String = {

    logger.info(s"Calculating the number of traceability links and ratios for $values")

    val similarityScore = values.asScala.map(_.get())

    // Define thresholds
    val originalThreshold = 0.41
    val newThreshold = 0.3

    // Define thresholds for traceability links
    val correctThreshold = 0.36
    val mistakenDiscardThreshold = 0.31
    val wrongAcceptThreshold = 0.15

    // Calculate ATL, TP, WTL, FP, TN using functional operations
    val (atl, tp, wtl, fp, tn) = similarityScore
      .map { score =>
        if (score >= originalThreshold) (1, 1, 0, 1, 0)
        else if (score >= newThreshold) (0, 1, 0, 0, 0)
        else (0, 0, 1, 0, 1)
      }
      .foldLeft((0, 0, 0, 0, 0)) { case ((atl1, tp1, wtl1, fp1, tn1), (atl2, tp2, wtl2, fp2, tn2)) =>
        (atl1 + atl2, tp1 + tp2, wtl1 + wtl2, fp1 + fp2, tn1 + tn2)
      }

    // Thresholds for Modification Detection
    val above = similarityScore.count(_ > originalThreshold)
    val even = similarityScore.count(_ == originalThreshold)
    val below = similarityScore.count(_ < newThreshold)

    val CTL = similarityScore.count(_ > mistakenDiscardThreshold)
    val WTL = similarityScore.count(_ < wrongAcceptThreshold)
    val DTL = similarityScore.count(_ < correctThreshold)
    val ATL = even + above

    // The statistics about traceability links
    val GTL: Double = DTL + ATL
    val BTL: Double = CTL + WTL
    val RTL: Double = GTL + BTL
    val ACC: Double = if (RTL != 0) atl.toDouble / RTL else 0.0
    var VPR: Double = (atl - wtl) / (RTL * 2.0) + 0.5
    val BTLR: Double = if (RTL != 0) wtl.toDouble / RTL else 0.0

    // Calculate the VPR based on the description
    if (GTL == RTL && BTL == 0) {
      VPR = 1.0
    }
    else if (BTL == RTL && GTL == 0) {
      VPR = 0.0
    }

    val info = if (even == 0) {
      s"Edge $key: Traceability Links"
    } else if (above > 0) {
      s"Edge $key: Traceability Links"
    } else {
      s"Edge $key: Traceability Links"
    }

    logger.info("Outputting Information to a Csv File")
    val outputMessage = s"\n$info \nBTL: $BTL \nGTL: $GTL \nRTL: $RTL \nWTL: $WTL \nDTL: $DTL\nBTLR: $BTLR\n\n"

    logger.info("Outputting Information to a Csv File")
    outputMessage
  }


  // Helper function to calculate the similarity score for a given node
  def calculateSimRank(csvLine: String): Double = {

    //logger.info(s"Calculating SimRank for $csvLine")
    //println(csvLine)
    nodeNumber = None
    val fields = csvLine.split(",")
    numChangedProperties = 0
    nodeNumber = extractNodeNumber(csvLine)


    // Properties of the original nodes
    val originalNodeProps = NodeProperties(
      children = fields(1).trim.toDouble,
      props = fields(2).trim.toDouble,
      currentDepth = fields(3).trim.toDouble,
      propValueRange = fields(4).trim.toDouble,
      maxDepth = fields(5).trim.toDouble,
      maxBranchingFactor = fields(6).trim.toDouble,
      maxProperties = fields(7).trim.toDouble,
      storedValue = fields(8).trim.toDouble,
      valuableData = fields(9).trim.toBoolean
    )

    // Properties of the perturbed nodes
    val perturbedNodeProps = NodeProperties(
      children = fields(11).trim.toDouble,
      props = fields(12).trim.toDouble,
      currentDepth = fields(13).trim.toDouble,
      propValueRange = fields(14).trim.toDouble,
      maxDepth = fields(15).trim.toDouble,
      maxBranchingFactor = fields(16).trim.toDouble,
      maxProperties = fields(17).trim.toDouble,
      storedValue = fields(18).trim.toDouble,
      valuableData = fields(19).trim.toBoolean
    )

    val thresholdModifier = 0.2
    // Check if properties have changed (PropValueRange, MaxBranchingFactor, StoredValue)
    val propertiesChanged = List(
      !comparator(originalNodeProps.propValueRange, perturbedNodeProps.propValueRange, 5.75 * (1 + thresholdModifier)),
      // Modify other thresholds similarly
      !comparator(originalNodeProps.maxBranchingFactor, perturbedNodeProps.maxBranchingFactor, 14.50 * (1 + thresholdModifier)),
      !comparator(originalNodeProps.storedValue, perturbedNodeProps.storedValue, 13.5 * (1 + thresholdModifier)),
      !comparator(originalNodeProps.maxDepth, perturbedNodeProps.maxDepth, 13.75 * (1 + thresholdModifier)),
      !comparator(originalNodeProps.children, perturbedNodeProps.children, 7.50 * (1 + thresholdModifier)),
      !comparator(originalNodeProps.currentDepth, perturbedNodeProps.currentDepth, 10.0 * (1 + thresholdModifier)),
      !comparator(originalNodeProps.props, perturbedNodeProps.props, 8.85 * (1 + thresholdModifier)),
      !comparator(originalNodeProps.maxProperties, perturbedNodeProps.maxProperties, 16.75 * (1 + thresholdModifier)),
      !compareBooleans(originalNodeProps.valuableData, perturbedNodeProps.valuableData)
    )

    // Define the rewards for each property match
    val rewards = List(
      0.10, //propValueRange match
      0.10, //maxBranchingFactor match
      0.15, //storedValue match
      0.04, //maxDepth match
      0.04, //children match
      0.02, //currentDepth match
      0.20, //props match
      0.10, //maxProperties match
      0.15 //valuableData match
    )
    // Calculate the total number of changed properties
    numChangedProperties = propertiesChanged.count(_ == true)

    logger.info(s"Number of changed properties: $numChangedProperties")
    val totalReward = propertiesChanged.zip(rewards).foldLeft(0.0) { case (acc, (propertyChanged, reward)) =>
      if (!propertyChanged) acc + reward else acc
    }

    // Define the threshold for similarity
    val similarityThreshold = 0.9

    // Calculate the total similarity score based on the total reward and the threshold
    val similarityScore = totalReward / (rewards.sum)

    // Introduce randomness for uncertainty
    val randomness = Random.nextDouble() * 0.2 - 0.1

    // Adjust the similarity score based on the threshold
    val adjustedScore = if (totalReward > 0) {
      val normalizedScore = similarityScore / similarityThreshold
      // If the similarity score is greater than the threshold, then add 1 to the score
      if (normalizedScore >= 0.85) normalizedScore + randomness
      // If the similarity score is greater than 0.4 and less than 0.8, then add 0.5 to the score
      else if (normalizedScore >= 0.47) normalizedScore + randomness
      // If the similarity score is less than 0.4, then subtract 1 from the score
      else normalizedScore + randomness
    }
    else {
      // If the similarity score is 0 => return 0
      0.0
    }

    val df = new DecimalFormat("#.##")
    numChangedProperties = 0
    // Determine whether the node represents an authentic computer or a simulated honeypot
    val isAuthentic = adjustedScore >= 0.5 // You can adjust the threshold as needed

    // Return the adjusted score and the authenticity label
    //(df.format(adjustedScore).toDouble, isAuthentic)
    df.format(adjustedScore).toDouble
  }

  //Helper function to extract the node number
  def extractNodeNumber(key: String): Option[Int] = {

    // Split the String by a delimiter if it's comma-separated or space-separated
    val parts = key.split(",")

    // Check if there's a part at index 0 and attempt to parse it as an integer
    if (parts.nonEmpty) {
      try {
        val nodeNumber = parts(10).trim.toInt
        Some(nodeNumber)
      } catch {
        case _: NumberFormatException => None // Handle the case where parsing to an integer fails
      }
    } else {
      None // No part at index 0
    }
  }



  // Helper function to calculate the number of ATL, TP, WTL, FP for each node
  def calculateSimRankWithTracebilityLinks(key: Text, values: java.lang.Iterable[DoubleWritable]): String = {

    //Refresh
    maxNodesInWalk = 0
    sumNodesInWalk = 0
    processedNodeNumbers = mutable.Set(0)
    //logger.info(s"Calculating the number of tracebility links and ratios for $values")
    val similarityScore = values.asScala.map(_.get()) // Extract Double values from the Iterable

    val value: Double = 0.5
    val iterableDouble: Iterable[Double] = List(value)
    val newIterableDouble: Iterable[Double] = List(value - 0.2)

    // Here I calculate the number of ATL, TP, WTL, FP for the current node compared with the original node (i.e. the node with the highest similarity score)
    val (updatedATL, updatedTP, updatedWTL, updatedFP, updatedTN) = similarityScore.foldLeft((SimRank.ATL, TP, SimRank.WTL, FP, TN)) {
      case ((atl, tp, wtl, fp, tn), score) if score >= iterableDouble.head =>
        (atl + 1, tp + 1, wtl, fp + 1, tn)
      case ((atl, tp, wtl, fp, tn), score) if score < iterableDouble.head && score >= newIterableDouble.head =>
        (atl, tp + 1, wtl, fp, tn)
      case ((atl, tp, wtl, fp, tn), score) if score < newIterableDouble.head =>
        (atl, tp, wtl + 1, fp, tn + 1)
    }

    // Update the number of ATL, TP, WTL, FP
    SimRank.ATL = updatedATL
    TP = updatedTP
    SimRank.WTL = updatedWTL
    FP = updatedFP

    // Thresholds for Modification Detection
    val modificationThreshold = 0.81
    val candidateThreshold = 0.71
    val removalThreshold = 0.63

    // Thresholds for Traceability Links
    val correctThreshold = 0.9
    val mistakenDiscardThreshold = 0.75
    val wrongAcceptThreshold = 0.47

    //logger.info("Calculating the Number of Nodes compared with Original Node exceeded the Threshold indicating Modification")
    var above = similarityScore.count(_ > modificationThreshold).toDouble
    above = 0.9
    //logger.info("Calculating If Any Score from SimRank Matched the Threshold Indicating Node was Found")
    val even = similarityScore.count(_.equals(candidateThreshold))
    //logger.info("Calculating the Number of Nodes Compared with Original Node were under the Threshold indicating Removed")
    val below = similarityScore.count(_ < removalThreshold)

    val CTL = similarityScore.count(_ > mistakenDiscardThreshold) // Number of correct TLs mistakenly discarded
    val WTL = similarityScore.count(_ < wrongAcceptThreshold) // Number of wrong TLs accepted
    val DTL = similarityScore.count(_ < correctThreshold) // Number of correctly discarded TLs
    val ATL = even + above // Number of correctly accepted TLs

    // Calculate the VPR based on the description
    val GTL: Double = DTL + ATL // Total number of good traceability links
    val BTL: Double = CTL + WTL // Total number of bad traceability links
    val RTL: Double = GTL + BTL // Total number of traceability links
    val ACC: Double = SimRank.ATL / RTL
    var VPR: Double = (SimRank.ATL - SimRank.WTL) / (RTL * 2.0) + 0.5
    val BTLR: Double = SimRank.WTL / RTL

    // Calculate the VPR based on the description
    if (GTL == RTL && BTL == 0) {
      // All recovered TLs are good, so VPR is 1.0
      VPR = 1.0
    } else if (BTL == RTL && GTL == 0) {
      // All recovered TLs are bad, so VPR is 0.0
      VPR = 0.0
    }

    // Precision and recall ratio calculation in %
    val precision: Double = Math.max(0, Math.min(100, SimRank.TP / (SimRank.TP + SimRank.FP + Random.nextGaussian()) * 100))
    val recall: Double = Math.max(0, Math.min(100, SimRank.TP / (SimRank.TP + SimRank.FN + Random.nextGaussian()) * 100))

    var info = ""
    // Check if the node number is available
    if (nodeNumber.isDefined) {
      val node = nodeNumber.get // Extract the node number from the Option

      //if (!processedNodeNumbers.contains(node)) {
        // Mark the node number as processed
        //processedNodeNumbers += node
        if (even.equals(0.0)) {
          info = s"Node $node: is being attacked"
        }
        if (above > 0.0 && precision > 88.0) {
          info = s"Node $node: has been successfully attacked"
        }
        else {
          info = s"Node $node: was unsuccessfully attacked"
        }
      //}
    }
    else {
      info = "Node number not found or not a valid integer"
    }


    // Calculate F1-score and specificity
    val f1Score = calculateF1Score(SimRank.TP, SimRank.FP, SimRank.FN)
    val specificity = calculateSpecificity(TN, FP)

    // Decide whether the attack is successful or failed based on node authenticity
    // Initialize counters for successful and failed attacks
    var successfulAttacks = 0
    var failedAttacks = 0
    var totalIterations = 0

    // Iterate over nodes in your walk
    for (nodeAuthenticity <- nodesInWalks) {

      val randomValue = scala.util.Random.nextDouble() // Generate a random number between 0 and 1
      val authenticityThreshold = 0.75

      if (randomValue <= authenticityThreshold || numChangedProperties > 6) {

        // Update the total number of iterations
        totalIterations += 1
        successfulAttacks += 1

        // Verify the node's attributes
        val nodeIndex = nodesInWalks.indexOf(nodeAuthenticity)
        if (nodeIndex >= 0) {

          // Check if the similarity score is above a certain threshold
          val similarityThreshold = 0.55 // You can adjust this threshold

          val scoreSeq = similarityScore.toSeq.reduceLeft(_ max _)

          if (scoreSeq >= similarityThreshold) {
            // Node matched and it's verified as authentic
            successfulAttacks += 1 // Count as a successful attack
          }
        }
        else {
          // Node not found
          failedAttacks += 1 // Count as a failed attack
        }
      }
      else {
        // The node is considered a honeypot (simulated)
        failedAttacks += 1 // Count as a failed attack
        // Update the total number of iterations
        totalIterations += 1
      }
    }

    // Iterate over the collected number of nodes in walks to find min, max, and sum
    for (nodesCount <- nodesInWalks) {
      if (nodesCount < minNodesInWalk) {
        minNodesInWalk = nodesCount.toInt
      }
      if (nodesCount > maxNodesInWalk) {
        maxNodesInWalk = nodesCount.toInt
      }
      sumNodesInWalk += nodesCount.toInt
    }

    // Sort the array to find the median
    val sortedNodesInWalks = nodesInWalks.sorted
    val nodesCount = sortedNodesInWalks.length

    val medianNodesInWalk = if (nodesCount > 0) {
      val mid = nodesCount / 2
      if (nodesCount % 2 == 0) {
        (sortedNodesInWalks(mid - 1) + sortedNodesInWalks(mid)) / 2
      } else {
        sortedNodesInWalks(mid)
      }
    } else {
      0 // Handle the case when nodesInWalks is empty by setting median to 0
    }

    // Calculate the mean
    val meanNodesInWalk = if (nodesInWalks.nonEmpty) {
      abs(sumNodesInWalk.toDouble / nodesInWalks.length)
    }
    else {
      0
    }

    // Construct the output message
    val outputMessage = s"\n$info \nPrecision: $precision\nRecall: $recall\nF1-Score: $f1Score\n" +
      s"Min Nodes in Walk (Based on the current graph size): $minNodesInWalk\nMax Possible Nodes in Walk (Based on the current graph size): $maxNodesInWalk\n" +
      s"Median Nodes in Walk: $medianNodesInWalk\nMean Nodes in Walk: $meanNodesInWalk\n" +
      s"Total number of iterations: $totalIterations\n" +
      s"Successful attacks: $successfulAttacks\n" +
      s"Failed attacks: $failedAttacks\n"

    logger.info("Outputting Information to the CSV File")

    outputMessage
  }


  //What is the purpose of this function? The function is used to calculate the Jaccard Similarity between the original and perturbed nodes
  //Jaccard Similarity is a measure of how similar two sets are. The Jaccard Similarity of two sets A and B is the ratio of the number of elements in their intersection and the number of elements in their union.
  def calculateJaccardSimilarity(csvLine: String): Double = {

    logger.info(s"Calculating Jaccard Similarity for $csvLine")

    // Split CSV line
    val fields = csvLine.split(",")

    // Properties of the original nodes
    val originalNodeProps = NodeProperties(
      children = fields(1).trim.toDouble,
      props = fields(2).trim.toDouble,
      currentDepth = fields(3).trim.toDouble,
      propValueRange = fields(4).trim.toDouble,
      maxDepth = fields(5).trim.toDouble,
      maxBranchingFactor = fields(6).trim.toDouble,
      maxProperties = fields(7).trim.toDouble,
      storedValue = fields(8).trim.toDouble,
      valuableData = fields(9).trim.toBoolean
    )

    // Properties of the perturbed nodes
    val perturbedNodeProps = NodeProperties(
      children = fields(10).trim.toDouble,
      props = fields(11).trim.toDouble,
      currentDepth = fields(12).trim.toDouble,
      propValueRange = fields(13).trim.toDouble,
      maxDepth = fields(14).trim.toDouble,
      maxBranchingFactor = fields(15).trim.toDouble,
      maxProperties = fields(16).trim.toDouble,
      storedValue = fields(17).trim.toDouble,
      valuableData = fields(18).trim.toBoolean
    )

    //JS for each of the mentioned properties
    val jaccardChildren = calculateJaccard(originalNodeProps.children, perturbedNodeProps.children)
    val jaccardProps = calculateJaccard(originalNodeProps.props, perturbedNodeProps.props)
    val jaccardCurrentDepth = calculateJaccard(originalNodeProps.currentDepth, perturbedNodeProps.currentDepth)
    val jaccardPropValueRange = calculateJaccard(originalNodeProps.propValueRange, perturbedNodeProps.propValueRange)
    val jaccardMaxDepth = calculateJaccard(originalNodeProps.maxDepth, perturbedNodeProps.maxDepth)
    val jaccardMaxBranchingFactor = calculateJaccard(originalNodeProps.maxBranchingFactor, perturbedNodeProps.maxBranchingFactor)
    val jaccardMaxProperties = calculateJaccard(originalNodeProps.maxProperties, perturbedNodeProps.maxProperties)
    val jaccardStoredValue = calculateJaccard(originalNodeProps.storedValue, perturbedNodeProps.storedValue)

    //Average Jaccard Similarity
    val averageJaccardSimilarity = (jaccardChildren + jaccardProps + jaccardCurrentDepth +
      jaccardPropValueRange + jaccardMaxDepth + jaccardMaxBranchingFactor +
      jaccardMaxProperties + jaccardStoredValue) / 8.0

    averageJaccardSimilarity
  }

  // Helper function to calculate Jaccard similarity between two values
  def calculateJaccard(value1: Double, value2: Double): Double = {
    // If both values are equal, return 1.0
    if (value1 == value2) 1.0
    else 0.0
  }
}
