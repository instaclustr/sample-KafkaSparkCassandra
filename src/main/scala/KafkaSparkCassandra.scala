/*
 *
 * Instaclustr (www.instaclustr.com)
 * Kafka, Spark Streaming and Cassandra example
 *
 */

// Imports for loading in the kafka.properties file
import java.io.FileReader
import java.util.Properties
import scala.collection.JavaConversions._

// Basic Spark imports
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

// Spark SQL Cassandra imports
import com.datastax.spark.connector._

// Spark Streaming + Kafka imports
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010._

// Cassandra Java driver imports
import com.datastax.driver.core.Cluster

// Date import for processing logic
import java.util.Date

object KafkaSparkCassandra {

  def main(args: Array[String]) {

    // read the configuration file
    val sparkConf = new SparkConf().setAppName("WordCount")

    // get the values we need out of the config file
    val cassandra_host = sparkConf.get("spark.cassandra.connection.host"); //cassandra host
    val cassandra_user = sparkConf.get("spark.cassandra.auth.username")
    val cassandra_pass = sparkConf.get("spark.cassandra.auth.password")

    // connect directly to Cassandra from the driver to create the keyspace
    val cluster = Cluster.builder().addContactPoint(cassandra_host).withCredentials(cassandra_user, cassandra_pass).build()
    val session = cluster.connect()
    session.execute("CREATE KEYSPACE IF NOT EXISTS ic_example WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };")
    session.execute("CREATE TABLE IF NOT EXISTS ic_example.word_count (word text, ts timestamp, count int, PRIMARY KEY(word, ts)) ")
    session.execute("TRUNCATE ic_example.word_count")
    session.close()

    // Create spark streaming context with 5 second batch interval
    val ssc = new StreamingContext(sparkConf, Seconds(5))

    // Set the logging level to reduce log message spam
    ssc.sparkContext.setLogLevel("ERROR")

    // create a timer that we will use to stop the processing after 60 seconds so we can print some results
    val timer = new Thread() {
      override def run() {
        Thread.sleep(1000 * 30)
        ssc.stop()
      }
    }

    // load the kafka.properties file
    val kafkaProps = new Properties()
    kafkaProps.load(new FileReader("kafka.properties"))
    val kafkaParams = kafkaProps.toMap[String, String]

    // Create direct Kafka stream on the wordcount-input topic
    val topicsSet = Set[String]("wordcount-input")
    val messages = KafkaUtils.createDirectStream(ssc,PreferConsistent,Subscribe[String,String](topicsSet,kafkaParams))

    // Create the processing logic
    // the spark processing isn't actually run until the streaming context is started
    // it will then run once for each batch interval

    // Get the lines, split them into words, count the words and print
    val wordCounts = messages.map(_.value) // split the message into lines
      .flatMap(_.split(" ")) //split into words
      .filter(w => w.length() > 0) // remove any empty words caused by double spaces
      .map(w => (w, 1L)).reduceByKey(_ + _) // count by word
      .map({case (w,c) => (w,new Date().getTime,c)}) // add the current time to the tuple for saving

    wordCounts.print() //print it so we can see something is happening

    // Save each RDD to the ic_example.word_count table in Cassandra
    wordCounts.foreachRDD(rdd => {
      rdd.saveToCassandra("ic_example","word_count")
    })

    // Now we have set up the processing logic it's time to do some processing
    ssc.start() // start the streaming context
    timer.start()
    ssc.awaitTermination() // block while the context is running (until it's stopped by the timer)
    ssc.stop() // this additional stop seems to be required

    // Get the results using spark SQL
    val sc = new SparkContext(sparkConf) // create a new spark core context
    val rdd1 = sc.cassandraTable("ic_example", "word_count")
    rdd1.take(100).foreach(println)
    sc.stop()

    System.exit(0)
  }
}
