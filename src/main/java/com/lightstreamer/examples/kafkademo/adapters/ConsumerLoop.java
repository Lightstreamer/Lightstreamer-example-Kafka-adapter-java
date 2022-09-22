package com.lightstreamer.examples.kafkademo.adapters;


import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

public class ConsumerLoop extends Thread {

    private String kafkabootstrapstring;

    private String kafkaconsumergroupid;

    private String ktopicname;

    private boolean goconsume;

    private KafkaDataAdapter dapter;

    private static Logger logger = LogManager.getLogger("kafkademo-adapters");

    public ConsumerLoop(KafkaDataAdapter dapter, String kafka_bootstrap_string, String kgroupid, String topicname) {
        this.kafkabootstrapstring = kafka_bootstrap_string;
        this.kafkaconsumergroupid = kgroupid;
        this.ktopicname = topicname;
        this.dapter = dapter;
        this.goconsume = true;
    }

    public void stopconsuming() {
        this.goconsume = false;
    }

    @Override
    public void run() {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", kafkabootstrapstring);
        props.setProperty("group.id", kafkaconsumergroupid);
        props.setProperty("enable.auto.commit", "true");
        props.setProperty("auto.commit.interval.ms", "1000");
        props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        try {
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Arrays.asList(ktopicname));
            while (goconsume) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(7000));
                for (ConsumerRecord<String, String> record : records) {
                    logger.info("offset = " + record.offset() + ", key = " + record.key() + ", value = " + record.value());
                    if ( record.key().equalsIgnoreCase("current_time")) {
                        dapter.onCurrentTimeUpdate(record.value());
                    } else {
                        dapter.onNewMessage(record.key(), record.value());
                    }

                }  
                logger.info("wait for new messages");
            }
            logger.info("End consumer loop");
        } catch (Exception e) {
            logger.error("Error during consumer loop: " + e.getMessage());
        }
        
    }
    
}
