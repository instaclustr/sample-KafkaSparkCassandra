name := "cassandra-kafka-streaming"

version := "1.0"

scalaVersion := "2.10.5"

libraryDependencies += "org.apache.spark" %% "spark-core" % "2.1.1" % "provided"

libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.1.1" % "provided"

libraryDependencies += ("com.datastax.spark" %% "spark-cassandra-connector" % "2.0.1").exclude("io.netty", "netty-handler")

libraryDependencies += ("com.datastax.cassandra" % "cassandra-driver-core" % "3.0.0").exclude("io.netty", "netty-handler")

libraryDependencies += "org.apache.spark" %% "spark-streaming" % "2.1.1" % "provided"

libraryDependencies += ("org.apache.spark" %% "spark-streaming-kafka" % "1.6.3").exclude("org.spark-project.spark", "unused")

assemblyMergeStrategy in assembly := {
 case PathList("META-INF", xs @ _*) => MergeStrategy.discard
 case x => MergeStrategy.first
}
