package GraphGeneration

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.graphx.GraphXUtils

/**
 * Provides a method to run tests against a `SparkContext` variable that is correctly stopped
 * after each test.
 */
trait LocalSparkContext {
  /** Runs `f` on a new SparkContext and ensures that it is stopped afterwards. */
  def withSpark[T](f: SparkContext => T): T = {
    val conf = new SparkConf()
    GraphXUtils.registerKryoClasses(conf)
    val sc = new SparkContext("local", "test", conf)
    try {
      f(sc)
    }
    finally {
      sc.stop()
      sc.parallelize(Seq.empty[Int]).count() // force stop
    }
  }
}