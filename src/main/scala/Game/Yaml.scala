package Game

import Game.GameLogic.{Move, Moves}
import NetGraphAlgebraDefs.NodeObject
import GameLogic.{Move, Moves}
import org.yaml.snakeyaml.{DumperOptions, Yaml}

import scala.jdk.CollectionConverters._
import java.io.{FileWriter, PrintWriter}


//Object containing functions for handling YAML parsing and conversion.
object Yaml {

  /**
   * Parses the String 'node' into a NodeObject.
   * @param node The String representation of the node.
   * @return     The parsed NodeObject.
   */
  def parseNode(node: String): NodeObject = {
    val fields = node.split(",").map(_.trim)

    // Ensure that there are exactly 10 fields
    if (fields.length == 10) {
      // Parse each field and create a NodeObject
      val id = fields(0).toInt
      val children = fields(1).toInt
      val props = fields(2).toInt
      val currentDepth = fields(3).toInt
      val propValueRange = fields(4).toInt
      val maxDepth = fields(5).toInt
      val maxBranchingFactor = fields(6).toInt
      val maxProperties = fields(7).toInt
      val storedValue = fields(8).toDouble
      val valuableData = fields(9).toBoolean

      NodeObject(id, children, props, currentDepth, propValueRange, maxDepth, maxBranchingFactor, maxProperties, storedValue, valuableData)
    } else {
      // Throw an exception or handle the error based on your requirements
      throw new IllegalArgumentException("Invalid input string format")
    }
  }

  /**
   * Converts Moves to a YAML string.
   * @param moves The Moves object to be converted.
   * @return      The YAML representation of the Moves.
   */
  def convertMovesToYaml(moves: Moves): String = {
    val movesMap = new java.util.HashMap[String, Any]()
    movesMap.put("adjacentNodes", moves.adjacentNodes.map(nodeToMap).asJava.toArray)
    movesMap.put("confidenceScore", moves.confidenceScore)
    movesMap.put("distanceToValuableNode", moves.distanceToValuableNode)
    movesMap.put("moves", moves.moves.map(moveToMap).asJava.toArray)
    movesMap.put("pathToValuableNode", moves.pathToValuableNode.map(nodeToMap).asJava.toArray)

    val yamlOptions = new DumperOptions()
    yamlOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
    val yaml = new Yaml(yamlOptions)

    // Convert MovesMap to a YAML string
    yaml.dump(movesMap)
  }

  /**
   * Converts a NodeObject to a Map for YAML representation.
   * @param node The NodeObject to be converted.
   * @return     The Map representation of the NodeObject.
   */
  def nodeToMap(node: NodeObject): java.util.Map[String, Any] = {
    val nodeMap = new java.util.HashMap[String, Any]()
    nodeMap.put("id", node.id)
    nodeMap.put("children", node.children)
    nodeMap.put("currentDepth", node.currentDepth)
    nodeMap.put("maxBranchingFactor", node.maxBranchingFactor)
    nodeMap.put("maxDepth", node.maxDepth)
    nodeMap.put("maxProperties", node.maxProperties)
    nodeMap.put("propValueRange", node.propValueRange)
    nodeMap.put("props", node.props)
    nodeMap.put("storedValue", node.storedValue)
    nodeMap.put("valuableData", node.valuableData)
    nodeMap
  }

  /**
   * Converts a Move to a Map for YAML representation.
   * @param move The Move object to be converted.
   * @return     The Map representation of the Move.
   */
  def moveToMap(move: Move): java.util.Map[String, Any] = {
    val moveMap = new java.util.HashMap[String, Any]()
    moveMap.put("node", nodeToMap(move.node))
    moveMap.put("userId", move.userId)
    moveMap
  }

  /**
   * Writes YAML to a file.
   * @param filePath   The path to the YAML file.
   * @param yamlString The YAML string to be written to the file.
   */
  def writeYamlToFile(filePath: String, yamlString: String): Unit = {
    val writer = new PrintWriter(new FileWriter(filePath))
    try {
      writer.write(yamlString)
    } finally {
      writer.close()
    }
  }
}

