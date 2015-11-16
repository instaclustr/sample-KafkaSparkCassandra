name := "cassandra-kafka-streaming"

version := "1.0"

scalaVersion := "2.10.5"

libraryDependencies += "org.apache.spark" %% "spark-core" % "1.4.1" % "provided"

libraryDependencies += "org.apache.spark" %% "spark-sql" % "1.4.1" % "provided"

libraryDependencies += ("com.datastax.spark" %% "spark-cassandra-connector" % "1.4.0").exclude("io.netty", "netty-handler")

libraryDependencies += ("com.datastax.cassandra" % "cassandra-driver-core" % "2.1.9").exclude("io.netty", "netty-handler")

libraryDependencies += "org.apache.spark" %% "spark-streaming" % "1.4.1" % "provided"

libraryDependencies += ("org.apache.spark" %% "spark-streaming-kafka" % "1.4.1").exclude("org.spark-project.spark", "unused")



