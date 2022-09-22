package com.lightstreamer.examples.kafkademo.producer;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
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

    private static String kconnstring; 

    private static boolean go = true;

    private static Random random = new Random();

    private static Calendar calendar = Calendar.getInstance();

    private static String topicname;

    private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

    private static HashMap<String, Integer> flight_momentum = new HashMap<String, Integer>();
    
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

    private static List<String> status_desc = Stream.of("Scheduled - On-time",
    "Scheduled - Delayed",
    "En Route - On-time",
    "En Route - Delayed",
    "Landed - On-time",
    "Landed - Delayed",
    "Cancelled",
    "Deleted").collect(Collectors.toList());

    private static List<String> status_icon = Stream.of("\uD83C\uDFAB",
    "⌛",
    "\uD83D\uDEEB",
    "\uD83D\uDEEC️",
    "✅",
    "\uD83D\uDFE9",
    "\uD83D\uDED1",
    "\uD83D\uDED1").collect(Collectors.toList());

    private static void kafkaproducerloop() {
        Properties props = new Properties();
        props.put("bootstrap.servers", kconnstring);
        props.put("linger.ms", 1);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        try {
            Future<RecordMetadata> futurek;
            RecordMetadata rmtdta;

            calendar.setTime(sdf.parse("07:00"));

            Producer<String, String> producer = new KafkaProducer<>(props);

            while (go) {
                String key;

                if ( flight_momentum.size() < 10 ) {
                    int tmp = random.nextInt(900) + 10;
                
                    key = "LS" + tmp;
                } else {
                    int fd = random.nextInt(10);
                    Iterator<String> itr = flight_momentum.keySet().iterator();
                    for (int k=0; k < fd; k++) {
                        itr.next();
                    }
                    key = itr.next();
                }

                String message = getrandominfo(key);

                logger.info("New Message : " + message);

                futurek = producer.send(new ProducerRecord<String, String>(topicname, key, message));

                rmtdta = futurek.get();

                futurek = producer.send(new ProducerRecord<String, String>(topicname, "current_time", sdf.format(calendar.getTime())));

                rmtdta = futurek.get();

                logger.info("Sent message to" + rmtdta.partition());

                Thread.sleep(500);
            }
            
             producer.close();

        } catch (Exception e) {
            logger.error("Error during producer loop: " + e.getMessage());
        }
    }

    private static int nextFlightStatus(int from) {
        int to = from;

        if (from == 0) {
            if ( random.nextBoolean() ) {
                to = 2;
            } else {
                to = 1;
            }
        } else if (from == 1) {
            if ( random.nextBoolean() ) {
                to = 3;
            } else {
                to = 6;
            }
        } else if (from == 2) {
            if ( random.nextBoolean() ) {
                to = 3;
            } else {
                to = 4;
            }
        } else if (from == 3) {
            to = 5;
        } else {
            // The Flight should be removed by the departures board
            to = 7;
        }

        return to;
    }

    private static String getrandominfo(String key) {
        StringBuilder builder = new StringBuilder();
        int inds;

        if (flight_momentum.containsKey(key)) {
            inds = nextFlightStatus(flight_momentum.get(key).intValue());
        } else {
            inds = 0;
        }

        if (inds == 7) {
            flight_momentum.remove(key);
        } else {
            flight_momentum.put(key, Integer.valueOf(inds));
        }

        int indx = random.nextInt(20);
        
        String trmnl = random.nextBoolean() ? "3" : "7";

        calendar.add(Calendar.MINUTE, 3);
        String departure = sdf.format(calendar.getTime());
        
        builder.append(status_icon.get(inds) + ' ' + destinations.get(indx))
            .append("|")
            .append(departure)
            .append("|")
            .append(trmnl)
            .append("|")
            .append( status_desc.get(inds))
            .append("|")
            .append("Lightstreamer Airlines");

        return builder.toString();
    }
    public static void main(String[] args) {
        
        logger.info("Start Kafka demo producer: " + args.length);

        if (args.length < 2 ) {
            logger.error("Missing arguments bootstrap-servers topioc-name");
            return ;
        }

        kconnstring = args[0];
        topicname = args[1];

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                kafkaproducerloop();
            }
        });  
        t1.start();
        
        String input = System.console().readLine();
        while (!input.equalsIgnoreCase("stop")) {
            input = System.console().readLine();
        }

        go = false;

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            // ...
        }
        

        logger.info("End Kafka demo producer.");
    }
}
