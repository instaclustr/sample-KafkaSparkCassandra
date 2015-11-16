/*
 *
 * Instaclustr (www.instaclustr.com)
 * Kafka, Spark Streaming and Casssandra example
 *
 */


// Basic Spark imports
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.SparkContext._

// Spark SQL Cassandra imports
import org.apache.spark.sql
import org.apache.spark.sql.cassandra.CassandraSQLContext
import org.apache.spark.sql.cassandra._
import com.datastax.spark.connector._

// Spark Streaming + Kafka imports
import kafka.serializer.StringDecoder // this has to come before streaming.kafka import
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._

// Cassandra Java driver imports
import com.datastax.driver.core.{Session, Cluster, Host, Metadata}
import com.datastax.spark.connector.streaming._
import scala.collection.JavaConversions._

// Date import for processing logic
import java.util.Date


object KafkaSparkCassandra {

  def main(args: Array[String]) {

    // read the configuration file
    val sparkConf = new SparkConf().setAppName("KafkaSparkCassandra")

    // get the values we need out of the config file
    val kafka_broker = "localhost:9092"
    val kafka_topic = "test"
    val cassandra_host = sparkConf.get("spark.cassandra.connection.host"); //cassandra host
    val cassandra_user = sparkConf.get("spark.cassandra.auth.username");
    val cassandra_pass = sparkConf.get("spark.cassandra.auth.password");

    // connect directly to Cassandra from the driver to create the keyspace
    val cluster = Cluster.builder().addContactPoint(cassandra_host).withCredentials(cassandra_user, cassandra_pass).build()
    val session = cluster.connect()
    session.execute("CREATE KEYSPACE IF NOT EXISTS ic_example WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };")
    session.execute("CREATE TABLE IF NOT EXISTS ic_example.word_count (word text, ts timestamp, count int, PRIMARY KEY(word, ts)) ")
    session.execute("TRUNCATE ic_example.word_count")
    session.close()

    // Create spark streaming context with 5 second batch interval
    val ssc = new StreamingContext(sparkConf, Seconds(5))

    // create a timer that we will use to stop the processing after 30 seconds so we can print some results
    val timer = new Thread() {
      override def run() {
        Thread.sleep(1000 * 30)
        ssc.stop()
      }
    }

    // Create direct kafka stream with brokers and topics
    val topicsSet = Set[String] (kafka_topic)
    val kafkaParams = Map[String, String]("metadata.broker.list" -> kafka_broker)
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc, kafkaParams, topicsSet)

    // Create the processing logic
    // the spark processing isn't actually run until the streaming context is started
    // it will then run once for each batch interval

    // Get the lines, split them into words, count the words and print
    val wordCounts = messages.map(_._2) // split the message into lines
      .flatMap(_.split(" ")) //split into words
      .filter(w => (w.length() > 0)) // remove any empty words caused by double spaces
      .map(w => (w, 1L)).reduceByKey(_ + _) // count by word
      .map({case (w,c) => (w,new Date().getTime(),c)}) // add the current time to the tuple for saving

    wordCounts.print() //print it so we can see something is happening

    // insert the records from  rdd to the ic_example.word_count table in Cassandra
    // SomeColumns() is a helper class from the cassandra connector that allows the fields of the rdd to be mapped to the columns in the table
    wordCounts.saveToCassandra("ic_example", "word_count", SomeColumns("word" as "_1", "ts" as "_2", "count" as "_3"))


    // Now we have set up the processing logic it's time to do some processing
    ssc.start() // start the streaming context
    timer.start() // start the thread that will stop the context processing after a while
    ssc.awaitTermination() // block while the context is running (until it's stopped by the timer)
    ssc.stop() // this additional stop seems to be required

    // Get the results using spark SQL
    val sc = new SparkContext(sparkConf) // create a new spark core context
    val csc = new CassandraSQLContext(sc) // wrap the base context in a Cassandra SQL context
    val rdd1 = csc.sql("SELECT * from ic_example.word_count") // select the data from the table
    rdd1.show(100) // print the first 100 records from the result
    sc.stop()

  }
}