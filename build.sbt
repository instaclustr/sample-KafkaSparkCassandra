name := "cassandra-kafka-streaming"

version := "1.0"

scalaVersion := "2.10.5"

libraryDependencies += "org.apache.spark" %% "spark-core" % "1.6.0" % "provided"

libraryDependencies += "org.apache.spark" %% "spark-sql" % "1.6.0" % "provided"

libraryDependencies += ("com.datastax.spark" %% "spark-cassandra-connector" % "1.6.0-M1").exclude("io.netty", "netty-handler")

libraryDependencies += ("com.datastax.cassandra" % "cassandra-driver-core" % "3.0.0").exclude("io.netty", "netty-handler")

libraryDependencies += "org.apache.spark" %% "spark-streaming" % "1.6.0" % "provided"

libraryDependencies += ("org.apache.spark" %% "spark-streaming-kafka" % "1.6.0").exclude("org.spark-project.spark", "unused")

