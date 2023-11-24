package org

import NetGraphAlgebraDefs.{NetGraph, NodeObject}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.directives.RouteDirectives
import org.GameLogic.Puppets.{latestPolicemanNode, latestThiefNode}
import org.GameLogic.Puppets

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Random


//Object containing conditional functions for the game
object Behavior {


  //Checks and handles the ultimate conditions that lead to the end of the game.
  def ultimateConditions(): Unit = {

    /**
     * Helper function to handle game over conditions.
     * @param player The player causing the game over.
     * @param reason The reason for the game over.
     */
    def gameOverMessage(player: String, reason: String): Unit = {
      restartGame()
      complete(StatusCodes.BadRequest, s"$player $reason. Game over.")
    }

    // Retrieve successors for Thief and Policeman
    val thiefSuccessors = GraphHolder.netOriginalGraph.sm.asGraph().successors(latestThiefNode)
    val policemanSuccessors = GraphHolder.netOriginalGraph.sm.asGraph().successors(latestPolicemanNode)

    // Check and handle game over conditions
    if (thiefSuccessors.isEmpty)
      gameOverMessage("Thief", "has no available moves")

    if (policemanSuccessors.isEmpty)
      gameOverMessage("Policeman", "has no available moves")

    if (latestThiefNode != null && latestThiefNode.valuableData)
      gameOverMessage("Thief", "has stolen the valuable data! Restarting...")
  }

  //Restarts the game by updating the latestThiefNode and latestPolicemanNode.
  def restartGame(): Unit = {

    //Positions initialized to random nodes
    Puppets.latestThiefNode = findStartingPositions(Puppets.graph)
    Puppets.latestPolicemanNode = findStartingPositions(Puppets.graph)
    RouteDirectives.redirect("/server/restart", StatusCodes.Found)
  }

  /**
   * Finds and returns a random starting position for the game.
   * @param graph The graph representing the game.
   * @return      A randomly selected starting position.
   */
  def findStartingPositions(graph: NetGraph): NodeObject = {
    val allNodes = graph.sm.asGraph().nodes().asScala.toList
    val random = new Random()

    // Randomly select a node from the list of all nodes
    val randomNode = allNodes(random.nextInt(allNodes.size))

    randomNode
  }

  //Displays a welcome message and instructions for the game
  def welcome(): Unit = {
    println(s"Server online at http://localhost:8080/\nPress ENTER to stop...\n")

    println("Welcome to the 'Hunter and the Lamb' game!\n" +
      "Please, select your player type: Thief or Policeman.\n" +
      "Next, open another terminal and, using 'curl' command, enter the node you want to move to.\n\n" +
      "Directions:\n" +
      "1. To make a move use the following, where node is the node you want to move to:\n curl -X PUT \"http://localhost:8080/server?node=328,5,13,1,31,3,6,15,0.6100300599846812,false&user=policeman\"\n" +
      "2. To get the current state of the game use the following, where playerId is either thief or policeman:\n curl -X GET \"http://localhost:8080/server?playerId=thief\"\n\n" +
      "Rules:\n" +
      "1. Both Thief and Policeman can only move to adjacent nodes.\n" +
      "2. If Thief reaches the node where the valuable data is stored, Thief wins.\n" +
      "3. If Policeman reaches the node where Thief is, Policeman wins.\n" +
      "4. The first player with no available moves left loses the game.\n" +
      "5. Thief starts the game first. Players take turns after each move.\n")

    println(s"Possible available adjacent nodes: \n" +
      s"Thief: ${GraphHolder.netOriginalGraph.sm.adjacentNodes(Puppets.latestThiefNode).asScala.toList}\n" +
      s"Policeman: ${GraphHolder.netOriginalGraph.sm.adjacentNodes(Puppets.latestPolicemanNode).asScala.toList}\n\n")

    //Helper function to handle the immediate execution of the game
    def immediateExecution(): Unit = {
      restartGame()
      println("New nodes are: " + Puppets.latestThiefNode + " " + Puppets.latestPolicemanNode)
      println("Possible moves for Thief: " + GraphHolder.netOriginalGraph.sm.adjacentNodes(Puppets.latestThiefNode).asScala.toList)
      println("Possible moves for Policeman: " + GraphHolder.netOriginalGraph.sm.adjacentNodes(Puppets.latestPolicemanNode).asScala.toList)
    }

    //Basic game rules
    if(Puppets.latestThiefNode.valuableData){
      println("Thief has stolen the valuable data! Game over.\n" +
        "New nodes are being generated...")
      immediateExecution()
    }

    if(GraphHolder.netOriginalGraph.sm.asGraph().adjacentNodes(Puppets.latestThiefNode).isEmpty){
      println("Thief has no available moves! Game over.\n" +
        "New nodes are being generated...")
      immediateExecution()
    }

    if(GraphHolder.netOriginalGraph.sm.asGraph().adjacentNodes(Puppets.latestPolicemanNode).isEmpty){
      println("Policeman has no available moves! Game over.\n" +
        "New nodes are being generated...")
      immediateExecution()
    }

    if(Puppets.latestPolicemanNode == Puppets.latestThiefNode){
      println("Policeman has caught the Thief! Game over.\n" +
        "New nodes are being generated...")
      immediateExecution()
    }

    // Sanity check
    require(GraphHolder.netOriginalGraph.sm.asGraph().nodes().asScala.nonEmpty, "Graph is empty!")
  }
}

