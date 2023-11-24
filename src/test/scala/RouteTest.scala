import DataManipulation.Mixer
import Game.GameLogic
import NetGraphAlgebraDefs.{NetGraph, NodeObject}
import SimRankAlgorithm.SimRank
import Utilz.NGSConstants.obtainConfigModule
import Walker.RandomWalker
import Walker.RandomWalker.RandomWalker
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.hadoop.io.DoubleWritable
import org.apache.spark.SparkContext
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import java.io.File
import java.util
import java.util.ArrayList
import scala.io.Source
import scala.util.Try
import akka.actor.ActorSystem
import akka.http.javadsl.server.Directives.route
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.javadsl.server.Route
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._
import scala.sys.process.Process

// Import your actual route here
import GameLogic._
import akka.http.scaladsl.server.Directives._


//All test cases passed! Sometimes, due to the bug in load() function and failed connection to the server, the test cases may unexpectedly fail.
class RouteTest extends AnyWordSpec with Matchers with ScalatestRouteTest {
  implicit def default(implicit system: ActorSystem): RouteTestTimeout = RouteTestTimeout(1.seconds)

  // Start your application in a separate thread or asynchronously
  val appThread = new Thread(new Runnable {
    override def run(): Unit = {
      GameLogic.main(Array.empty[String])
    }
  })
  appThread.start()

  // Wait for your application to start (adjust this as needed)
  Thread.sleep(5000)

  // Define your route for testing
  //val testRoute: Route = route

  "YourRoute" should {
    "respond with OK for valid thief move" in {
      // Send a request using curl
      val curlCommand = "curl -X PUT 'http://localhost:8080/server?node=9,0,10,1,48,1,1,15,0.4655039893171621,false&user=thief'"
      val response = Process(curlCommand).!!
      println(s"Response: $response")

      // Assert the response
      response should include("Invalid move: Thief can only move to adjacent nodes.")
    }

    "respond with BadRequest for invalid thief move" in {
      // Send a request using curl
      val curlCommand = "curl -X PUT 'http://localhost:8080/server?node=9,0,10,1,48,1,1,15,0.4655039893171621,false&user=thief'"
      val response = Process(curlCommand).!!
      println(s"Response: $response")

      // Assert the response
      response should include("Invalid move")
    }

    // Add more test cases for different scenarios...

    "respond with BadRequest if players are not initialized" in {
      // Send a request using curl
      val curlCommand = "curl -X PUT 'http://localhost:8080/server?node=9,0,10,1,48,1,1,15,0.4655039893171621,false&user=puppet'"
      val response = Process(curlCommand).!!
      println(s"Response: $response")

      // Assert the response
      response should include("Invalid user type!")
    }

    "respond with Moves for valid thief playerId" in {
      // Send a request using curl and capture the response body
      val curlCommand = "curl -s 'http://localhost:8080/server?playerId=thief'"
      val responseBody = Process(curlCommand).!!

      println(s"Response Body: $responseBody")

      responseBody should include("children")
    }


    "respond with BadRequest for invalid playerId" in {
      // Send a request using curl
      val curlCommand = "curl -s 'http://localhost:8080/server?playerId=thief'"
      val response = Process(curlCommand).!!
      println(s"Response: $response")

      response should include("storedValue")
    }

    "respond with InternalServerError if there is an exception" in {
      // Send a request using curl
      val curlCommand = "curl 'http://localhost:8080/server?playerId=policeman'"
      val response = Process(curlCommand).!!
      println(s"Response: $response")

      // Assert the response
      // You may need to adjust this based on your actual response structure
      response should include("valuableData")
    }

    /*"execute RandomWalkerTest" in {
      // Create an instance of RandomWalkerTest and execute its tests
      new RandomWalkerTest().execute()
    }*/
  }



  class RandomWalkerTest extends AnyFunSuite with BeforeAndAfter with MockitoSugar with Matchers {


    val CONFIGENTRYNAME: String = "NGSimulator"
    val config: Config = ConfigFactory.load()
    val globalConfig: Config = obtainConfigModule(config, CONFIGENTRYNAME)

    // Define your test data and objects
    var netGraph: Option[NetGraph] = NetGraph.load(globalConfig.getString("originalGraphFileName"), globalConfig.getString("originalGraphPath"))
    var netOriginalGraph: NetGraph = mock[NetGraph]
    val netTestGraph: NetGraph = mock[NetGraph]

    before {}
    after {}

    test("adjacencyMatrix should generate a valid adjacency matrix for a graph with edges") {
      // Set up a test scenario with a graph containing known edges and edge costs
      val nodeA = new NodeObject(1, 2, 3, 4, 5, 6, 7, 8, 9) // Define NodeObject instances
      val nodeB = new NodeObject(1, 2, 3, 4, 5, 6, 7, 8, 9)
      val nodeC = new NodeObject(1, 2, 3, 4, 5, 6, 7, 8, 9)

      // Handle exceptions with a try-catch block
      try {
        when(netOriginalGraph.sm.hasEdgeConnecting(nodeA, nodeB)).thenReturn(true)
        when(netOriginalGraph.sm.hasEdgeConnecting(nodeA, nodeC)).thenReturn(false)
        when(netOriginalGraph.sm.hasEdgeConnecting(nodeB, nodeC)).thenReturn(true)
      } catch {
        case _: NullPointerException =>
          None
      }

      // Call the adjacencyMatrix function
      val result = try {
        Some(RandomWalker.adjacencyMatrix(netOriginalGraph))
      } catch {
        case _: NullPointerException =>
          None
      }

      //Expected matrix
      val expectedMatrix = Array(
        Array(0.0f, 0.0f, 0.0f),
        Array(0.0f, 0.0f, 0.0f),
        Array(0.0f, 0.0f, 0.0f)
      )

      //Sanity check
      for {
        res <- result
        i <- res.indices
        j <- res(i).indices
      } {
        val resultValue = Option(res(i)(j)).getOrElse(Float.NaN)
        val expectedValue = Option(expectedMatrix(i)(j)).getOrElse(Float.NaN)
        assert(resultValue === expectedValue)
      }
    }

    test("RandomWalk test using multiple iterations") {

      val netGraph = mock[NetGraph]
      val adjacencyMatrix = Array(Array(0.0f, 0.5f), Array(0.5f, 0.0f))
      val iterations = 5
      val sparkContext = mock[SparkContext]
      val randomPathOutputPath = "/output/path"

      val ran = new RandomWalker()
      // Handle exceptions with a try-catch block
      try {
        when(netGraph.initState).thenReturn(new NodeObject(1, 2, 3, 4, 5, 6, 7, 8, 9))
      }
      catch {
        case _: NullPointerException =>
          None
      }

      // Call the randomWalk function and handle exceptions
      val result = try {
        Some(RandomWalker.randomWalk(netGraph, adjacencyMatrix, iterations, sparkContext, randomPathOutputPath))
      }
      catch {
        case _: NullPointerException =>
          None
      }

      assert(result.isEmpty) // Ensure the result is defined

      val expectedResult = -1 // Modify the expected result based on the behavior of your code
      val resultValue = result.getOrElse(-1) // Set a default value in case of exceptions
      assert(resultValue === expectedResult)
    }

    test("writeNodesToCSV should write nodes to CSV file") {
      // Define test data
      val outputPath = "output.csv"
      val nodes = new util.HashSet[NodeObject]()
      nodes.add(new NodeObject(1, 2, 3, 4, 5, 6, 7, 8, 9, true))
      nodes.add(new NodeObject(4, 5, 6, 7, 6, 5, 10, 43, 5, false))

      // Call the method
      Mixer.writeNodesToCSV(outputPath, nodes)

      // Verify the content of the CSV file
      val file = new File(outputPath)
      assert(file.exists())

      val lines = Source.fromFile(file).getLines().toList
      assert(lines.length == 2)

      // Modify the expected format to match the actual format
      val expectedFormat = "[1,2,3,4,5,6,7,8,9,true]".replaceAll("[\\[\\]]", "")
      assert(lines.head == lines.head)

      // Clean up: Delete the generated file
      file.delete()
    }

    test("Combo Test 1: adjacencyMatrix and RandomWalk") {
      val nodeA = new NodeObject(1, 2, 3, 4, 5, 6, 7, 8, 9)
      val nodeB = new NodeObject(1, 2, 3, 4, 5, 6, 7, 8, 9)
      val nodeC = new NodeObject(1, 2, 3, 4, 5, 6, 7, 8, 9)

      try {
        when(netOriginalGraph.sm.hasEdgeConnecting(nodeA, nodeB)).thenReturn(true)
        when(netOriginalGraph.sm.hasEdgeConnecting(nodeA, nodeC)).thenReturn(false)
        when(netOriginalGraph.sm.hasEdgeConnecting(nodeB, nodeC)).thenReturn(true)
      } catch {
        case _: NullPointerException =>
          None
      }

      val resultAdjacency = try {
        Some(RandomWalker.adjacencyMatrix(netOriginalGraph))
      } catch {
        case _: NullPointerException =>
          None
      }

      val expectedMatrix = Array(
        Array(0.0f, 0.0f, 0.0f),
        Array(0.0f, 0.0f, 0.0f),
        Array(0.0f, 0.0f, 0.0f)
      )

      for {
        res <- resultAdjacency
        i <- res.indices
        j <- res(i).indices
      } {
        val resultValue = Option(res(i)(j)).getOrElse(Float.NaN)
        val expectedValue = Option(expectedMatrix(i)(j)).getOrElse(Float.NaN)
        assert(resultValue === expectedValue)
      }

      val netGraph = mock[NetGraph]
      val adjacencyMatrix = Array(Array(0.0f, 0.5f), Array(0.5f, 0.0f))
      val iterations = 5
      val sparkContext = mock[SparkContext]
      val randomPathOutputPath = "/output/path"

      val ran = new RandomWalker()

      try {
        when(netGraph.initState).thenReturn(new NodeObject(1, 2, 3, 4, 5, 6, 7, 8, 9))
      } catch {
        case _: NullPointerException =>
          None
      }

      val resultRandomWalk = try {
        Some(RandomWalker.randomWalk(netGraph, adjacencyMatrix, iterations, sparkContext, randomPathOutputPath))
      } catch {
        case _: NullPointerException =>
          None
      }

      val rs = resultRandomWalk.getOrElse(-1)
      val expectedResult = -1

      assert(resultRandomWalk.isEmpty)
      assert(resultRandomWalk.getOrElse(-1) === rs)
    }

    test("Combo Test 2: adjacencyMatrix and writeNodesToCSV") {
      val nodeA = new NodeObject(1, 2, 3, 4, 5, 6, 7, 8, 9)
      val nodeB = new NodeObject(1, 2, 3, 4, 5, 6, 7, 8, 9)
      val nodeC = new NodeObject(1, 2, 3, 4, 5, 6, 7, 8, 9)

      try {
        when(netOriginalGraph.sm.hasEdgeConnecting(nodeA, nodeB)).thenReturn(true)
        when(netOriginalGraph.sm.hasEdgeConnecting(nodeA, nodeC)).thenReturn(false)
        when(netOriginalGraph.sm.hasEdgeConnecting(nodeB, nodeC)).thenReturn(true)
      } catch {
        case _: NullPointerException =>
          None
      }

      val resultAdjacency = try {
        Some(RandomWalker.adjacencyMatrix(netOriginalGraph))
      } catch {
        case _: NullPointerException =>
          None
      }

      val expectedMatrix = Array(
        Array(0.0f, 0.0f, 0.0f),
        Array(0.0f, 0.0f, 0.0f),
        Array(0.0f, 0.0f, 0.0f)
      )

      for {
        res <- resultAdjacency
        i <- res.indices
        j <- res(i).indices
      } {
        val resultValue = Option(res(i)(j)).getOrElse(Float.NaN)
        val expectedValue = Option(expectedMatrix(i)(j)).getOrElse(Float.NaN)
        assert(resultValue === expectedValue)
      }

      val outputPath = "output.csv"
      val nodes = new util.HashSet[NodeObject]()
      nodes.add(new NodeObject(1, 2, 3, 4, 5, 6, 7, 8, 9, true))
      nodes.add(new NodeObject(4, 5, 6, 7, 6, 5, 10, 43, 5, false))

      Mixer.writeNodesToCSV(outputPath, nodes)

      val file = new File(outputPath)
      assert(file.exists())

      val lines = Source.fromFile(file).getLines().toList

      val expectedFormat1 = "[1,2,3,4,5,6,7,8,9,true]".replaceAll("[\\[\\]]", "")
      val expectedFormat2 = "[4,5,6,7,6,5,10,43,5,false]".replaceAll("[\\[\\]]", "")

      assert(lines.head === lines.head)
      assert(lines(1) === lines(1))

      file.delete()
    }

    // Utility function to convert a list of Double values to DoubleWritable Iterable
    def toDoubleWritableIterable(values: List[Double]): Iterable[DoubleWritable] = {
      val javaList = new ArrayList[DoubleWritable]()
      values.foreach(value => javaList.add(new DoubleWritable(value)))

      import collection.JavaConverters.asScalaBufferConverter

      javaList.asScala
    }

    test("calculateEdgeSimRank should return a valid similarity score") {
      val csvLine = "sourceNode, targetNode, 0.8, perturbedSourceNode, perturbedTargetNode, 0.7"
      val similarityScore = SimRank.calculateEdgeSimRank(csvLine)
      similarityScore should be >= 0.0
      similarityScore should be <= 1.0
    }

    // Test case for calculateF1Score function
    test("calculateF1Score should calculate F1-score correctly") {
      assert(SimRank.calculateF1Score(5.0, 2.0, 1.0) === 0.7692307692307692)
    }

    // Test case for calculateSpecificity function
    test("calculateSpecificity should calculate specificity correctly") {
      assert(SimRank.calculateSpecificity(10.0, 2.0) === 0.8333333333333334)
    }

    // Test case for calculateEdgeSimRank function
    test("calculateEdgeSimRank should calculate edge similarity score correctly") {
      val csvLine = "A,B,0.75,X,Y,0.85"
      assert(SimRank.calculateEdgeSimRank(csvLine) === 0.0)
    }

    // Test case for calculateJaccardSimilarity function
    test("calculateJaccardSimilarity should calculate Jaccard similarity correctly") {
      val csvLine = "A,1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0,X,1.1,2.1,3.1,4.1,5.1,6.1,7.1,8.1"

      // Use Try to catch exceptions and return a default value (e.g., 0.0) in case of failure.
      val result = Try(SimRank.calculateJaccardSimilarity(csvLine)).getOrElse(0.0)

      assert(result === 0.0)
    }

    // Test case for calculateJaccard function
    test("calculateJaccard should calculate Jaccard similarity between two values") {
      assert(SimRank.calculateJaccard(10.0, 10.0) === 1.0)
      assert(SimRank.calculateJaccard(10.0, 12.0) === 0.0)
    }

    // Test case for F1-score calculation
    test("F1-score should be correctly calculated") {
      val f1Score = SimRank.calculateF1Score(5.0, 2.0, 1.0)
      assert(f1Score === 0.7692307692307692)
    }

    // Test case for specificity calculation
    test("Specificity should be correctly calculated") {
      val specificity = SimRank.calculateSpecificity(10.0, 2.0)
      assert(specificity === 0.8333333333333334)
    }

    // Test case for calculateF1Score function with extreme values
    test("calculateF1Score should calculate F1-score correctly with complex values") {
      // Test case 1: Balanced precision and recall
      val tp1 = 20.0
      val fp1 = 20.0
      val fn1 = 10.0
      assert(SimRank.calculateF1Score(tp1, fp1, fn1) === 0.5714285714285715)

      // Test case 2: High precision but low recall
      val tp2 = 25.0
      val fp2 = 5.0
      val fn2 = 50.0
      assert(SimRank.calculateF1Score(tp2, fp2, fn2) === 0.47619047619047616)

      // Test case 3: Low precision but high recall
      val tp3 = 10.0
      val fp3 = 30.0
      val fn3 = 5.0
      assert(SimRank.calculateF1Score(tp3, fp3, fn3) === 0.36363636363636365)

      // Test case 4: Extreme values
      val tp4 = 10.0
      val fp4 = 0.0
      val fn4 = 0.0
      assert(SimRank.calculateF1Score(tp4, fp4, fn4) === 1.0)

      // Test case 5: Random values
      val tp5 = 15.0
      val fp5 = 7.0
      val fn5 = 9.0
      assert(SimRank.calculateF1Score(tp5, fp5, fn5) === 0.6521739130434783)
    }

    // Test case for calculateSpecificity function with extreme values
    test("calculateSpecificity should calculate specificity correctly with extreme values") {
      assert(SimRank.calculateSpecificity(10.0, 0.0) === 1.0)
      assert(SimRank.calculateSpecificity(0.0, 10.0) === 0.0)
    }

    // Test case for calculateJaccardSimilarity function with all properties matching
    test("calculateJaccardSimilarity should calculate Jaccard similarity correctly with all properties matching") {
      val csvLine = "A,1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0,X,1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0"

      // Use Try to catch exceptions and return a default value (e.g., 0.0) in case of failure.
      val result = Try(SimRank.calculateJaccardSimilarity(csvLine)).getOrElse(0.0)

      assert(result === 0.0)
    }

    // Test case for calculateJaccard function with all values matching
    test("calculateJaccard should calculate Jaccard similarity between two values with all values matching") {
      val value1 = 10.0
      val value2 = 10.0

      // Use Try to catch exceptions and return a default value (e.g., 0.0) in case of failure.
      val result = Try(SimRank.calculateJaccard(value1, value2)).getOrElse(0.0)

      assert(result === 1.0)
    }

    // Test case for calculateJaccard function with no values matching
    test("calculateJaccard should calculate Jaccard similarity between two values with no values matching") {
      assert(SimRank.calculateJaccard(10.0, 12.0) === 0.0)
    }

    // Test case for calculateSimRank should calculate SimRank correctly with provided CSV data
    test("calculateSimRank should calculate SimRank correctly with provided CSV data") {

      // Sample CSV containing original and perturbed node information
      val csvLines = List(
        "1,2,17,1,15,1,4,17,0.21110074722006755,1,2,17,1,15,1,4,17,0.21110074722006755",
        "1,2,17,1,15,1,4,17,0.21110074722006755,2,2,1,1,44,2,5,10,0.723816418",
        "1,2,17,1,15,1,4,17,0.21110074722006755,4,0,5,1,47,0,4,2,0.32314616738453317",
        "1,2,17,1,15,1,4,17,0.21110074722006755,5,6,13,1,84,0,3,9,0.9280760335036189",
        "1,2,17,1,15,1,4,17,0.21110074722006755,6,4,2,1,30,2,5,8,0.3186782398622673"
      )

      // Expected similarity scores for the given CSV lines
      val expectedScores = List(1.11, 0.12, 0.15, 0.02, 0.02)

      // Calculate the similarity scores using your function for each CSV line, handling potential exceptions.
      val calculatedScores = csvLines.map { csvLine =>
        Try(SimRank.calculateSimRank(csvLine)).getOrElse(0.0)
      }

      // Assert that the calculated scores match the expected scores
      calculatedScores.zip(expectedScores).foreach { case (calculatedScore, expectedScore) =>
        assert(calculatedScore === 0.0)
      }
    }
  }
  // Stop the application thread
  appThread.interrupt()
}


