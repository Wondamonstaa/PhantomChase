package MITMAttacher

import com.typesafe.config.ConfigFactory
import org.apache.spark._
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.slf4j.Logger

// Define VertexProperty as a trait or class
trait VertexProperty

// Define UserProperty
case class UserProperty(name: String, role: String) extends VertexProperty

// Create a new class that holds UserProperty and VertexProperty
case class UserVertex(id: VertexId, userProperty: UserProperty) extends VertexProperty

// Define ProductProperty
case class ProductProperty(name: String, price: Double) extends VertexProperty


object GraphApp {

  val logger: Logger = org.slf4j.LoggerFactory.getLogger("RandomWalker")
  val config = ConfigFactory.load()


  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .master("local[1]")
      .appName("MITMAttacher")
      .getOrCreate()

    val sc = spark.sparkContext

    val users: RDD[(Long, UserVertex)] = sc.parallelize(Seq(
      (3L, UserVertex(3L, UserProperty("rxin", "student"))),
      (7L, UserVertex(7L, UserProperty("jgonzal", "postdoc"))),
      (5L, UserVertex(5L, UserProperty("franklin", "prof"))),
      (2L, UserVertex(2L, UserProperty("istoica", "prof"))
      )))

    val relationships: RDD[Edge[String]] = sc.parallelize(Seq(
      Edge(3L, 7L, "collab"),
      Edge(5L, 3L, "advisor"),
      Edge(2L, 5L, "colleague"),
      Edge(5L, 7L, "pi")
    ))

    val defaultUser = UserVertex(0L, UserProperty("John Doe", "Missing"))

    val graph = Graph(users, relationships, defaultUser)

    val postdocCount = graph.vertices.filter { case (id, UserVertex(_, user)) => user.role == "postdoc" }.count()
    val edgeCount = graph.edges.filter(e => e.srcId > e.dstId).count()

    // Define the output path
    val outputPath = "/Users/wondamonsta/Desktop/UIC/2023/Fall2023/CS441CloudProject2/Spark_Output/output.txt"

    // Save the counts to a text file
    val outputRDD = sc.parallelize(Seq(s"Postdoc count: $postdocCount", s"Edge count where src > dst: $edgeCount"))
    logger.info(s"Saving output to: $outputPath")
    outputRDD.saveAsTextFile(outputPath)

    // Stop the Spark session
    spark.stop()
    logger.info("Spark session stopped")
  }
}
