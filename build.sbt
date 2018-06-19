name := "cassandra-kafka-streaming"

version := "1.0.1"

scalaVersion := "2.11.8"

libraryDependencies += "org.apache.spark" %% "spark-core" % "2.3.0" % "provided"

libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.3.0" % "provided"

libraryDependencies += ("com.datastax.spark" %% "spark-cassandra-connector" % "2.3.0").exclude("io.netty", "netty-handler")

libraryDependencies += "org.apache.spark" %% "spark-streaming" % "2.3.0" % "provided"

libraryDependencies += ("org.apache.spark" %% "spark-streaming-kafka-0-10" % "2.3.0").exclude("org.spark-project.spark", "unused")

libraryDependencies += "org.apache.kafka" % "kafka-clients" % "1.1.0"


assemblyMergeStrategy in assembly := {
 case PathList("META-INF", xs @ _*) => MergeStrategy.discard
 case x => MergeStrategy.first
}
