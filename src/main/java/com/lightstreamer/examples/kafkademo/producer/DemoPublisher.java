package com.lightstreamer.examples.kafkademo.producer;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DemoPublisher {

    private static Logger logger = LogManager.getLogger("kafkademo-producer");

    private static String kconnstring = "b-2.democluster1.rw4f0s.c9.kafka.eu-west-1.amazonaws.com:9092,b-1.democluster1.rw4f0s.c9.kafka.eu-west-1.amazonaws.com:9092";

    private static List<String> destinations = Stream.of( "Seoul (ICN)",
    "Atlanta (ATL)",
    "Boston (BOS)",
    "Phoenix (PHX)",
    "Detroit (DTW)",
    "San Francisco (SFO)",
    "Salt Lake City (SLC)",
    "Fort Lauderdale (FLL)",
    "Los Angeles (LAX)",
    "Seattle (SEA)",
    "Miami (MIA)",
    "Orlando (MCO)",
    "Charleston (CHS)",
    "West Palm Beach (PBI)",
    "Fort Myers (RSW)",
    "San Salvador (SAL)",
    "Tampa (TPA)",
    "Portland (PWM)",
    "London (LHR)",
    "Malpensa (MXP)").collect(Collectors.toList());

    private static List<String> statusf = Stream.of("Scheduled - On-time",
    "Scheduled - Delayed",
    "En Route - On-time",
    "En Route - Delayed",
    "Landed - On-time",
    "Landed - Delayed",
    "Cancelled").collect(Collectors.toList());

    private static void kafkaproducerloop() {
        Properties props = new Properties();
        props.put("bootstrap.servers", kconnstring);
        props.put("linger.ms", 1);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        try {
            Future<RecordMetadata> futurek;
            RecordMetadata rmtdta;

            Producer<String, String> producer = new KafkaProducer<>(props);
            for (int i = 0; i < 10; i++) {
                String message = getrandominfo(i);

                logger.info("New Message for " + i + ": " + message);

                futurek = producer.send(new ProducerRecord<String, String>("departuresboard-001", Integer.toString(i), message));

                rmtdta = futurek.get();

                logger.info("Sent message no. " + i + " to " + rmtdta.partition());
            }
            
            producer.close();

        } catch (Exception e) {
            logger.error("Error during producer loop: " + e.getMessage());
        }
    }
    private static String getrandominfo(int i) {
        StringBuilder builder = new StringBuilder();
        Random random = new Random();
        int indx = random.nextInt(21);
        int inds = random.nextInt(7);
                
        builder.append(destinations.get(indx))
            .append("|")
            .append("12:30")
            .append("|")
            .append( Integer.toString(indx))
            .append("|")
            .append( statusf.get(inds))
            .append("|")
            .append("Lightstreamer Airlines");

        return builder.toString();
    }
    public static void main(String[] args) {
        
        logger.info("Start Kafka demo producer.");

        kafkaproducerloop();

        logger.info("End Kafka demo producer.");
    }
}
