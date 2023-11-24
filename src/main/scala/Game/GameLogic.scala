package org

//Object imports
import org.Behavior._
import org.Statistics._
import org.Yaml._
import NetGraphAlgebraDefs.NetGraph.logger
import NetGraphAlgebraDefs.{NetGraph, NodeObject}
import org.GameLogic.Puppets.{latestPolicemanNodePerturbed, latestThiefNodePerturbed}
import org.apache.spark.sql.Dataset

//Spark imports
import org.apache.spark.sql.SparkSession
import org.apache.spark.rdd.RDD

//Akka imports
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import org.GameLogic.Puppets.{latestPolicemanNode, latestThiefNode}

//JSON
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

//Scala imports
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn
import scala.jdk.CollectionConverters._
import scala.concurrent.Await


//Game engine
object GameLogic {

  // Sealed trait representing messages that can be sent to actors
  sealed trait Message

  // Message class representing a move made by a player
  case class Move(userId: String, var node: NodeObject) extends Message

  // Message class representing a collection of moves and related information
  case class Moves(
                    moves: List[Move], // List of moves made by players
                    adjacentNodes: List[NodeObject], // List of adjacent nodes based on the latest node of the player
                    distanceToValuableNode: Double, // Distance to the nearest node with valuable data
                    pathToValuableNode: List[NodeObject], // Path to the nearest node with valuable data
                    confidenceScore: Double // Confidence score for the player's moves
                  ) {

    // Getter methods for accessing the fields
    def getMoves: List[Move] = moves

    def getAdjacentNodes: List[NodeObject] = adjacentNodes

    def getDistanceToValuableNode: Double = distanceToValuableNode

    def getPathToValuableNode: List[NodeObject] = pathToValuableNode

    def getConfidenceScore: Double = confidenceScore
  }


  //The following case class is used to store the moves of the players
  case class GetMoves(playerId: String, replyTo: ActorRef[Moves]) extends Message

  //Implicit JSON formats for the case classes
  implicit val nodeObjectFormat: RootJsonFormat[NodeObject] = jsonFormat(
    NodeObject.apply,
    "id", "children", "props", "currentDepth", "propValueRange",
    "maxDepth", "maxBranchingFactor", "maxProperties", "storedValue", "valuableData"
  )
  implicit val moveFormat: RootJsonFormat[Move] = jsonFormat2(Move.apply)
  implicit val movesFormat: RootJsonFormat[Moves] = jsonFormat5(Moves.apply)

  // Implicit JSON format for NodeObject using Spray JSON
  implicit object NodeObjectJsonFormat extends RootJsonFormat[NodeObject] {

    import spray.json.{JsNumber, JsObject, JsString, JsValue}

    // Serialization: Convert NodeObject to JSON
    def write(node: NodeObject): JsValue = JsObject(
      "id" -> JsNumber(node.id),
      "children" -> JsNumber(node.children),
      "props" -> JsNumber(node.props),
      "currentDepth" -> JsNumber(node.currentDepth),
      "propValueRange" -> JsNumber(node.propValueRange),
      "maxDepth" -> JsNumber(node.maxDepth),
      "maxBranchingFactor" -> JsNumber(node.maxBranchingFactor),
      "maxProperties" -> JsNumber(node.maxProperties),
      "storedValue" -> JsNumber(node.storedValue),
      "valuableData" -> JsString(node.valuableData.toString)
    )

    // Deserialization: Convert JSON to NodeObject
    def read(value: JsValue): NodeObject = value.asJsObject.getFields(
      "id", "children", "props", "currentDepth", "propValueRange", "maxDepth", "maxBranchingFactor", "maxProperties", "storedValue", "valuableData"
    ) match {
      case Seq(
      JsNumber(id),
      JsNumber(children),
      JsNumber(props),
      JsNumber(currentDepth),
      JsNumber(propValueRange),
      JsNumber(maxDepth),
      JsNumber(maxBranchingFactor),
      JsNumber(maxProperties),
      JsNumber(storedValue),
      JsString(valuableDataStr)
      ) =>
        NodeObject(
          id.toInt,
          children.toInt,
          props.toInt,
          currentDepth.toInt,
          propValueRange.toInt,
          maxDepth.toInt,
          maxBranchingFactor.toInt,
          maxProperties.toInt,
          storedValue.toDouble,
          valuableDataStr.toBoolean
        )
      case _ => throw new IllegalArgumentException("Invalid JSON format for NodeObject")
    }
  }


  //Actors section
  object Puppets {

    //Graph representations: original and perturbed
    val graph: NetGraph = GraphHolder.netOriginalGraph
    val perturbedGraph: NetGraph = GraphHolder.netPerturbedGraph

    // Track the latest nodes for each user: original graph
    var latestThiefNode: NodeObject = findStartingPositions(graph)
    var latestPolicemanNode: NodeObject = findStartingPositions(graph)

    //For testing purposes => Policeman arrests the thief immediately
    //var latestPolicemanNode: NodeObject = latestThiefNode
    //For testing purposes => Thief wins immediately
    //var latestThiefNode: NodeObject = NodeObject(395,2,9,1,65,0,6,6,0.8868890135357071,true)

    // Track the latest nodes for each user: perturbed graph
    var latestThiefNodePerturbed: NodeObject = findStartingPositions(perturbedGraph)
    var latestPolicemanNodePerturbed: NodeObject = findStartingPositions(perturbedGraph)

    /**
     * Defines the initial behavior for the actors.
     *
     * @return Initial behavior for handling messages.
     */
    def apply: Behavior[Message] = Behaviors.setup { context =>
      // Create actor references for the thief and policeman actors
      val thiefActor: ActorRef[Message] = context.spawn(ThiefActor(), "thiefActor")
      val policemanActor: ActorRef[Message] = context.spawn(PolicemanActor(), "policemanActor")

      // Initialize the behavior with default values
      applyBehavior(
        moves = List.empty[Move],
        thiefActor = thiefActor,
        policemanActor = policemanActor,
        ownNode = None,
        opponentNode = None,
        ownAdjacentNodes = List.empty[NodeObject],
        opponentAdjacentNodes = List.empty[NodeObject],
        ownConfidenceScore = 0.0,
        opponentConfidenceScore = 0.0,
        distanceToValuableNode = 0
      )
    }


    /**
     * Applies the behavior for handling moves and responding to GetMoves queries.
     *
     * @param moves                   List of previous moves.
     * @param thiefActor              ActorRef for the Thief.
     * @param policemanActor          ActorRef for the Policeman.
     * @param ownNode                 Optional NodeObject representing the current node of the player.
     * @param opponentNode            Optional NodeObject representing the current node of the opponent.
     * @param ownAdjacentNodes        List of adjacent nodes for the player.
     * @param opponentAdjacentNodes   List of adjacent nodes for the opponent.
     * @param ownConfidenceScore      Confidence score for the player.
     * @param opponentConfidenceScore Confidence score for the opponent.
     * @param distanceToValuableNode  Distance to the nearest node with valuable data.
     * @return Receive behavior for processing messages.
     */
    private def applyBehavior(
                               moves: List[Move],
                               thiefActor: ActorRef[Message],
                               policemanActor: ActorRef[Message],
                               ownNode: Option[NodeObject],
                               opponentNode: Option[NodeObject],
                               ownAdjacentNodes: List[NodeObject],
                               opponentAdjacentNodes: List[NodeObject],
                               ownConfidenceScore: Double,
                               opponentConfidenceScore: Double,
                               distanceToValuableNode: Int
                             ): Behaviors.Receive[Message] =

      Behaviors.receive {
        case (ctx, move@Move(userId, offer)) =>
          try {
            ctx.log.info(s"Move complete: $userId, $offer")
            val updatedMoves = moves :+ move

            val updatedOwnNode: Option[NodeObject] = userId match {
              case "thief" =>
                latestThiefNode = offer
                latestThiefNodePerturbed = offer
                Some(latestThiefNode)
              case "policeman" =>
                latestPolicemanNode = offer
                latestPolicemanNodePerturbed = offer
                Some(latestPolicemanNode)
            }

            val updatedOpponentNode: Option[NodeObject] = userId match {
              case "thief" =>
                Some(latestPolicemanNode)
              case "policeman" =>
                Some(latestThiefNode)
            }

            // Update the latest submitted node for each user
            userId match {
              case "thief" =>
                latestThiefNode = offer
                latestThiefNodePerturbed = offer
              case "policeman" =>
                latestPolicemanNode = offer
                latestPolicemanNodePerturbed = offer
            }

            applyBehavior(
              updatedMoves,
              thiefActor,
              policemanActor,
              updatedOwnNode,
              updatedOpponentNode,
              ownAdjacentNodes,
              opponentAdjacentNodes,
              ownConfidenceScore,
              opponentConfidenceScore,
              distanceToValuableNode
            )
          } catch {
            case e: Exception =>
              ctx.log.error(s"Error processing move: $move", e)
              logger.info(s"Error processing move: $move", e)
              Behaviors.same
          }

        case (_, GetMoves(playerId, replyTo)) =>
          // Filter moves based on the player ID
          val playerMoves = moves.filter(_.userId == playerId)

          // Compute the adjacent nodes of the specified player based on the latest node
          val playerAdjacentNodes = playerId match {
            case "thief" =>
              val adjacentNodesOption: Option[List[NodeObject]] =
                Option(latestThiefNode)
                  .map(node => GraphHolder.netOriginalGraph.sm.adjacentNodes(node).asScala.toList)
              adjacentNodesOption.getOrElse(List.empty[NodeObject])

            case "policeman" =>
              val adjacentNodesOption: Option[List[NodeObject]] =
                Option(latestPolicemanNode)
                  .map(node => GraphHolder.netOriginalGraph.sm.adjacentNodes(node).asScala.toList)
              adjacentNodesOption.getOrElse(List.empty[NodeObject])

            case _ => List.empty[NodeObject]
          }

          // Distance and path calculation
          val (distance: Double, path: List[NodeObject]) = playerId match {
            case "thief" => dijkstraShortestPath(latestThiefNode, GraphHolder.netPerturbedGraph)
            case "policeman" => dijkstraShortestPath(latestPolicemanNode, GraphHolder.netPerturbedGraph)
            case _ => (Int.MaxValue, List.empty[NodeObject])
          }

          // Confidence score calculation
          val confidenceScore = playerId match {
            case "thief" => calculateConfidenceScore(latestThiefNode, latestThiefNodePerturbed, GraphHolder.netPerturbedGraph)
            case "policeman" => calculateConfidenceScore(latestPolicemanNode, latestPolicemanNodePerturbed, GraphHolder.netPerturbedGraph)
            case _ => 0.0
          }

          replyTo ! Moves(playerMoves, playerAdjacentNodes, distance, path, confidenceScore)
          Behaviors.same
      }


    // Actor for Thief
    private object ThiefActor {
      case class MakeMove(userId: String, node: NodeObject) extends Message

      def apply(): Behavior[Message] = Behaviors.receiveMessage {
        case MakeMove(userId, node) =>
          // Simulate Thief making a bid
          // Process the bid and update the state accordingly
          println(s"Thief $userId makes a move to node $node")
          Behaviors.same
      }
    }

    // Actor for Policeman
    private object PolicemanActor {
      case class MakeMove(userId: String, node: NodeObject) extends Message

      def apply(): Behavior[Message] = Behaviors.receiveMessage {
        case MakeMove(userId, node) =>
          // Simulate Policeman making a bid
          if (latestPolicemanNode == null) {
            // Log a message to indicate that the latestPolicemanNode is null
            println("Latest Policeman node is null")
          } else if (GraphHolder.netOriginalGraph.sm.asGraph().successors(latestPolicemanNode).contains(node)) {
            println(s"Policeman $userId makes a move to node $node")
          } else {
            println(s"Invalid move: Policeman can only move to adjacent nodes. Current node: $latestPolicemanNode")
          }
          Behaviors.same
      }
    }
  }


  //@main
  def main(args: Array[String]): Unit = {


    //The entry point to programming Spark with the Dataset and DataFrame API.
    val spark = SparkSession.builder()
      .master("local[*]")
      .appName("Thief and Policeman")
      .getOrCreate() //Gets an existing session, if one exists

    implicit val system: ActorSystem[Message] = ActorSystem(Puppets.apply, "server")
    implicit val executionContext: ExecutionContext = system.executionContext
    val storage: ActorRef[Message] = system

    //To specify the turns of the players
    var currentPlayer: String = "thief"

    val route =
      pathPrefix("server") {
        concat(
          put {
            parameters("node", "user") { (node, user) =>

              //Sanity check for normal initialization
              if (Puppets.latestThiefNode != null && Puppets.latestPolicemanNode != null) {

                import akka.http.scaladsl.model.{HttpResponse, StatusCodes}

                //Basic conditions to satisfy before starting the game
                ultimateConditions()

                //Everything is okay, so we proceed
                user.toLowerCase match {
                  case "thief" =>

                    // Convert the String 'node' to a NodeObject
                    val parsedNode = parseNode(node)

                    val adjacentNodes = GraphHolder.netOriginalGraph.sm.adjacentNodes(parsedNode).asScala.toList

                    // Check if there are no available moves (adjacent nodes)
                    if (adjacentNodes.isEmpty) {
                      complete(StatusCodes.PreconditionFailed, "Thief has no available moves. Game over.")
                    }
                    else {

                      val successors = GraphHolder.netOriginalGraph.sm.asGraph().successors(latestThiefNode)
                      val predecessors = GraphHolder.netOriginalGraph.sm.asGraph().predecessors(latestThiefNode)

                      //Checking successors only since otherwise the game will be infinite
                      if(successors.isEmpty){
                        complete(StatusCodes.BadRequest, "Thief has no available moves. Game over.")
                      }

                      // Check if the latestThiefNode is not null
                      if (latestThiefNode != null) {
                        if (latestThiefNode.valuableData) {
                          complete {
                            welcome()
                            StatusCodes.BadRequest -> "Thief has stolen the valuable data! Game over. Restarting..."
                          }

                        }

                        // Check if the next move is valid
                        if (successors.contains(parsedNode)
                          || predecessors.contains(parsedNode)
                          || adjacentNodes.contains(parsedNode)) {

                          if (latestThiefNode.valuableData) {
                            complete {
                              welcome()
                              StatusCodes.BadRequest -> "Thief has stolen the valuable data! Game over. Restarting..."
                            }
                          }
                          else{

                            if(currentPlayer == "thief"){
                              // Send the move to the actor
                              storage ! Move("thief", parsedNode)
                              // Update latestThiefNode after the move is sent
                              latestThiefNode = parsedNode
                              latestThiefNodePerturbed = parsedNode

                              if (latestThiefNode.valuableData) {
                                complete {
                                  welcome()
                                  StatusCodes.BadRequest -> "Thief has stolen the valuable data! Game over. Restarting..."
                                }
                              }
                              else if (successors.isEmpty) {
                                complete {
                                  welcome()
                                  StatusCodes.BadRequest -> "Thief has no available moves. Game over."
                                }

                              }
                              else {
                                currentPlayer = "policeman" //FIXME: change to test thief only
                                complete(StatusCodes.Accepted,
                                  s"Thief move placed: ${parsedNode.id}\n" +
                                    s"Valuable data: ${parsedNode.valuableData}\n" +
                                    s"Next possible move valid: $adjacentNodes\n$successors\n" +
                                    s"Latest node: $latestThiefNode\n\n")
                              }
                            }
                            else{
                              complete(StatusCodes.BadRequest, "It's Policeman's turn!")
                            }
                          }
                        }
                        else {
                          complete(StatusCodes.BadRequest, s"Invalid move: " +
                            s"Thief can only move to adjacent nodes.\n" +
                            s"Current node: $latestThiefNode\n" +
                            s"Selected move: $parsedNode\n" +
                            s"Adjacent nodes of the current node: $successors\n\n")
                        }
                      }
                      // Winning condition check outside the if (latestThiefNode != null) block
                      else if (latestThiefNode != null && latestThiefNode.valuableData) {
                        complete {
                          welcome()
                          StatusCodes.BadRequest -> "Thief has stolen the valuable data! Game over. Restarting..."
                        }
                      }
                      else {
                        complete(StatusCodes.BadRequest, "Latest Thief node not initialized")
                      }
                    }

                  case "policeman" =>

                    // Convert the String 'node' to a NodeObject
                    val parsedNode = parseNode(node)

                    val adjacentNodes = GraphHolder.netOriginalGraph.sm.adjacentNodes(parsedNode).asScala.toList

                    // Check if there are no available moves (adjacent nodes)
                    if (adjacentNodes.isEmpty) {
                      complete(StatusCodes.BadRequest, "Policeman has no available moves. Game over.")
                    }
                    //Winning condition for the policeman
                    else if (latestPolicemanNode == latestThiefNode) {
                      complete {
                        welcome()
                        StatusCodes.BadRequest -> "Policeman has arrested the Thief! Game over. Restarting..."
                      }
                    }
                    else {

                      val successors = GraphHolder.netOriginalGraph.sm.asGraph().successors(latestPolicemanNode)
                      val predecessors = GraphHolder.netOriginalGraph.sm.asGraph().predecessors(latestPolicemanNode)

                      if (successors.isEmpty) {
                        complete {
                          welcome()
                          StatusCodes.BadRequest -> "Policeman has no available moves. Game over."
                        }
                      }

                      // Check if the latestPolicemanNode is not null
                      if (latestPolicemanNode != null) {
                        // Check if the next move is valid
                        if (successors.contains(parsedNode) || predecessors.contains(parsedNode) || adjacentNodes.contains(parsedNode)) {

                          if(currentPlayer == "policeman"){
                            // Send the move to the actor
                            storage ! Move("policeman", parsedNode)
                            // Update latestPolicemanNode after the move is sent
                            latestPolicemanNode = parsedNode
                            latestPolicemanNodePerturbed = parsedNode

                            if (latestPolicemanNode.equals(latestThiefNode)) {
                              complete {
                                welcome()
                                StatusCodes.BadRequest -> "Policeman has arrested the Thief! Game over. Restarting..."
                              }
                            }
                            else if (successors.isEmpty) {
                              complete {
                                welcome()
                                StatusCodes.BadRequest -> "Policeman has no available moves. Game over."
                              }
                            }
                            else {
                              currentPlayer = "thief"
                              complete(StatusCodes.Accepted,
                                s"Policeman move placed: ${parsedNode.id}\n" +
                                  s"Valuable data: ${parsedNode.valuableData}\n" +
                                  s"Next possible move valid: $adjacentNodes\n$successors\n" +
                                  s"Latest node: $latestPolicemanNode\n\n")
                            }
                          }
                          else{
                            complete(StatusCodes.BadRequest, "It's Thief's turn!")
                          }
                        }
                        else if(successors.isEmpty){
                          complete {
                            welcome()
                            StatusCodes.BadRequest -> "Policeman has no available moves. Game over."
                          }
                        }
                        else {
                          complete(StatusCodes.BadRequest, s"Invalid move: " +
                            s"Policeman can only move to adjacent nodes.\n" +
                            s"Current node: $latestPolicemanNode\n" +
                            s"Selected move: $parsedNode\n" +
                            s"Adjacent nodes of the current node: $successors\n\n")
                        }
                      } else {
                        complete(StatusCodes.BadRequest, "Latest Policeman node not initialized")
                      }
                    }
                  case _ =>
                    complete(StatusCodes.BadRequest, "Invalid user type!")
                }
              }
              else {
                complete(StatusCodes.BadRequest, "Players are not initialized!")
              }
            }
          },
          get {
            parameters("playerId") { playerId =>
              implicit val timeout: Timeout = 5.seconds

              // Query the actor for the current auction state
              val movesFuture: Future[Moves] = storage.ask(GetMoves(playerId, _))

              // Collect the results
              val moves: Moves = Await.result(movesFuture, 5.seconds)

              // Convert Moves to YAML
              val movesYaml = convertMovesToYaml(moves)

              logger.info("Creating the YAML file...")
              // Write the YAML to the file, overwriting existing content
              writeYamlToFile(GraphHolder.yamlPath, movesYaml)

              // Print the nodes for debugging
              logger.info(s"Latest Thief Node: $latestThiefNode")
              logger.info(s"Latest Policeman Node: $latestPolicemanNode")

              try {
                // Converts the Moves object to a Dataset of Strings
                import spark.implicits._
                val adjacentNodes: Dataset[String] = moves.getAdjacentNodes.map(move => s"Adjacent Node: $move").toDS()
                val score: Dataset[String] = Seq(s"\nConfidence Score: ${moves.getConfidenceScore}").toDS()
                val distance: Dataset[String] = Seq(s"\nDistance to Valuable Node: ${moves.getDistanceToValuableNode}").toDS()
                val movesDS: Dataset[String] = moves.getPathToValuableNode.map(move => s"Path to Valuable Node: $move").toDS()
                val movesList: Dataset[String] = moves.getMoves.map(move => s"Move: $move").toDS()

                // Combine the movesDS and score datasets
                val combinedData: Dataset[String] = movesDS.union(score).union(adjacentNodes).union(movesList).union(distance)

                // Save the combined dataset to a text file, overwriting the existing content
                combinedData.coalesce(1).write.mode("overwrite").text(GraphHolder.sparkPath)
              } catch {
                case e: Exception =>
                  logger.error("Error saving data to text file", e)
              } finally {
                // Stop the Spark session
                spark.stop()
              }

              // Return the Moves object as the response
              complete(moves)
            }
          },

          //The restart route for the game
          path("restart") {
            //get {
              restartGame()
              complete(StatusCodes.OK, "Game restarted.")
            //}
          }
        )
      }

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

    //Welcome message to the users
    welcome()

    //Basic conditions to satisfy before starting the game
    ultimateConditions()

    StdIn.readLine() // let it run until the user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done

    // Stop the Spark session.
    spark.stop()
  }
}



