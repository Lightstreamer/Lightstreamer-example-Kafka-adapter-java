# Lightstreamer - Kafka Demo - Java Adapter

This project includes the resources needed to develop the Data Adapter for the Lightstreamer Kafka Demo pluggable into Lightstreamer Server 

![Infrastructure](infrastructure.png)<br>

The Demo simulates a basic departures board with a few rows which represent information on flights departing from a hypothetical airport.
The data are simulated with a random generator provided in this project and sent to a [Kafka](https://kafka.apache.org/) topic; for this demo we used as Kafka service the [AWS MSK](https://aws.amazon.com/msk/?nc2=type_a).

This project covers only the back-end side of the demo and in particular the code of a Data Adapter to be deployed into a Lightsreamer server and a random data generator for simulating the departures board. For a client that displays this data, please refer to the section below: [Client to use with this demo](https://github.com/Lightstreamer/Lightstreamer-example-Kafka-adapter-java#client-to-use-with-this-demo).

## Details

The source code of the projects is basically divided into two packages: 

- `producer`, that implements the simulator of flight information and acts as the producer versus the Kafka service. In particular the following classes are defined:
    - `DemoPublisher.java`, implementing the simulator generating and sending flight monitor data to a Kafka topic;

<br>

- `adapters`, that implements the Lightstreamer in-process adapters based on the [Java In-Process Adapter API ](https://sdk.lightstreamer.com/ls-adapter-inprocess/7.3.1/api/index.html). in particular:
    - `KafkaDataAdapter.java` implements the Data Adapter publishing the simulated flights information;
    - `ConsumerLoop.java` implements a consumer loop for the Kafka service retrieving the messages to be pushed into the Lightstreamer server.

As per the Metadata adapter the demo relies on the basic functionalities provided by the ready made  [LiteralBasedProvider Metadata Adapter](https://github.com/Lightstreamer/Lightstreamer-lib-adapter-java-inprocess#literalbasedprovider-metadata-adapter).

## Build and Install

To build and install your own version of these adapters you have two options:
either use [Maven](https://maven.apache.org/) (or other build tools) to take care of dependencies and build (recommended) or gather the necessary jars yourself and build it manually.
For the sake of simplicity only the Maven case is detailed here.

### Maven

You can easily build the adapter jars to deploy into the Lightstreamer server using Maven through the `pom.xml` file located in the root folder of this project. As an alternative, you can use any other alternative build tool (e.g. Gradle, Ivy, etc.).

Assuming Maven is installed and available in your path you can build the demo by running
```sh 
 $mvn install dependency:copy-dependencies 
```

If the task completes successfully it also creates a `target` folder, with the jar of the adapter and all the needed dependencies.

## Setting up the Demo

The demo needs a kafka cluster where a topic with name `departuresboard-001` is defined. You can use a kafka server installed locally or any of the services offered in the cloud; for this demo we used [AWS MSK](https://aws.amazon.com/msk/?nc2=type_a) and this is exactly what the next steps refer to. 

### AWS MSK

 - Sign-in to the AWS Console in the account you want to create your cluster in 
 - Browse to the MSK create cluster wizard to start the creation 
 - Since the limited needs of the demo, you can choose options for a cluster with only 2 brokers, one per availability zone, and of small size (kafka.t3.small)
 - Choose Unauthenticated access option and allow Plaintext connection
 - We choose a cluster configuration such as the *MSK default configuration* but a single add; since in the demo only actually real-time events are managed we choose a very short retention time for messages:

```sh 
  log.retention.ms = 2000
```

 - [Create a topic](https://docs.aws.amazon.com/msk/latest/developerguide/create-topic.html) with name `departuresboard-001`.

### Lightstreamer Server

 - Download Lightstreamer Server (Lightstreamer Server comes with a free non-expiring demo license for 20 connected users) from [Lightstreamer Download page](https://lightstreamer.com/download/), and install it, as explained in the GETTING_STARTED.TXT file in the installation home directory.
 - Make sure that Lightstreamer Server is not running.
 - Get the deploy.zip file from the [latest release](https://github.com/Lightstreamer/Lightstreamer-example-Kafka-adapter-java/releases), unzip it, and copy the `kafkademo` folder into the `adapters` folder of your Lightstreamer Server installation.
 - Update the `adapters.xml` file setting the "kafka_bootstrap_servers" parameter with the connection string of your cluster created in the previous section; to retrieve this information use the steps below:
    1. Open the Amazon MSK console at https://console.aws.amazon.com/msk/.
    2. Wait for the status of your cluster to become Active. This might take several minutes. After the status becomes Active, choose the cluster name. This takes you to a page containing the cluster summary.
    3. Choose View client information.
    4. Copy the connection string for plaintext authentication.
 - [Optional] Customize the logging settings in log4j configuration file `kafkademo/classes/log4j2.xml`.
 - In order to avoid authentication stuff the machine running the Lightstreamer server must be in the same vpc of the MSK cluster.
 - Launch Lightstreamer Server.

### Simulator Producer loop

From the `LS_HOME\adapters\kafkademo\lib` folder you can start the simulator producer loop with this command 

```sh 
  $java -cp example-kafka-adapter-java-0.0.1-SNAPSHOT.jar:kafka-clients-3.2.2.jar:log4j-api-2.18.0.jar:log4j-core-2.18.0.jar:lz4-java-1.8.0.jar:snappy-java-1.1.8.4:slf4j-api-2.0.1.jar com.lightstreamer.examples.kafkademo.producer.DemoPublisher boostrap_server topic_name
```

Where *bootstrap_server* is the same information retrieved in the previous section and topic name is `departuresboard-001`.

### Client to use with this demo

As a client for this demo you can use the [Lightstreamer - DynamoDB Demo - Web Client](https://github.com/Lightstreamer/Lightstreamer-example-DynamoDB-client-javascript); you can follow the instructions in the [Install section](https://github.com/Lightstreamer/Lightstreamer-example-DynamoDB-client-javascript#install) with one addition:

 - change in the [src/js/const.js](https://github.com/Lightstreamer/Lightstreamer-example-DynamoDB-client-javascript/blob/master/src/js/const.js) file the *LS_ADAPTER_SET* to KAFKADEMO

## See Also

* Our blog post: [Virtual Airport Demo: Connecting Kafka to Lightstreamer](https://blog.lightstreamer.com/2023/03/virtual-airport-demo-connecting-kafka.html)

### Related Projects

* [LiteralBasedProvider Metadata Adapter](https://github.com/Lightstreamer/Lightstreamer-lib-adapter-java-inprocess#literalbasedprovider-metadata-adapter)

## Lightstreamer Compatibility Notes

- Compatible with Lightstreamer SDK for Java In-Process Adapters since 7.3.