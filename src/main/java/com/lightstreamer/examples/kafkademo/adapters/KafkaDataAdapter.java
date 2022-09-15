/*
  Copyright (c) Lightstreamer Srl

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package com.lightstreamer.examples.kafkademo.adapters;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lightstreamer.interfaces.data.DataProviderException;
import com.lightstreamer.interfaces.data.FailureException;
import com.lightstreamer.interfaces.data.ItemEventListener;
import com.lightstreamer.interfaces.data.DataProvider;
import com.lightstreamer.interfaces.data.SubscriptionException;


public class KafkaDataAdapter implements DataProvider {

    private static Logger logger = LogManager.getLogger("kafkademo-adapters");
    
    private static ItemEventListener listener;

    private static volatile boolean goconsume = false;

    private static List<String> keys = new ArrayList<String>();

    private static String kconnstring = "b-2.democluster1.rw4f0s.c9.kafka.eu-west-1.amazonaws.com:9092,b-1.democluster1.rw4f0s.c9.kafka.eu-west-1.amazonaws.com:9092";

    private void kafkaconsumerloop(ItemEventListener listener){
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", kconnstring);
        props.setProperty("group.id", "kafkademo-ls-001");
        props.setProperty("enable.auto.commit", "true");
        props.setProperty("auto.commit.interval.ms", "1000");
        props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        try {
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Arrays.asList("departuresboard-001"));
            while (goconsume) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(7000));
                for (ConsumerRecord<String, String> record : records) {
                    logger.info("offset = " + record.offset() + ", key = " + record.key() + ", value = " + record.value());
                    // onNewMessage(record.key(), record.value());
                    
                    logger.info("onNewMessage: " +  record.key() + " . " + record.value());

                    HashMap<String, String> update = new HashMap<String, String>();
                    // We have to set the key
                    update.put("key",  record.key());

                    if ( keys.contains( record.key()) ) {
                        // The UPDATE command
                        logger.info("update");
                        update.put("command", "UPDATE");
                    } else {            
                        // The ADD command
                        logger.info("add");
                        update.put("command", "ADD");
                        keys.add(record.key());
                    }
                    String[] tknz = record.value().split("\\|");
                    update.put("destination", tknz[0]);
                    update.put("departure", tknz[1]);
                    update.put("terminal", tknz[2]);
                    update.put("status", tknz[3]);
                    update.put("airline", tknz[4]);

                    listener.update("DepartureMonitor", update, false);
                }  
                logger.info("wait for new messages");
            }
            logger.info("End consumer loop");
        } catch (Exception e) {
            logger.error("Error during consumer loop: " + e.getMessage());
        }
    }

    private void kafkaproducerloop() {
        Properties props = new Properties();
        props.put("bootstrap.servers", kconnstring);
        props.put("linger.ms", 1);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        try {
            Future<RecordMetadata> futurek;
            RecordMetadata rmtdta;

            Producer<String, String> producer = new KafkaProducer<>(props);
            for (int i = 0; i < 2; i++) {
                futurek = producer.send(new ProducerRecord<String, String>("departuresboard-001", Integer.toString(i), "Test1"+i));

                rmtdta = futurek.get();

                logger.info("Sent message no. " + i + " to " + rmtdta.partition());
            }
            
            producer.close();

        } catch (Exception e) {
            logger.error("Error during producer loop: " + e.getMessage());
        }
    }

    @Override
    public void init(Map params, File configDir) throws DataProviderException {

        logger.info("Start initialization of the kafkademo Data adapter ... ");     

        logger.info("kakfademo Data Adapter initialized.");
    }

    @Override
    public void setListener(ItemEventListener listener) {
        
        this.listener = listener;
        
    }

    @Override
    public void subscribe(String itemName, boolean needsIterator) throws SubscriptionException, FailureException {

        logger.info("Subscribe for item: " + itemName);

        if (itemName.startsWith("DepartureMonitor")) {
            goconsume = true;
            Thread t1 = new Thread(new Runnable() {
                @Override
                public void run() {
                    logger.info("Start consumer loop " + listener);
                    kafkaconsumerloop(listener);
                }
            });  
            t1.start();
        }
        
    }

    @Override
    public void unsubscribe(String itemName) throws SubscriptionException, FailureException {

        if (itemName.startsWith("DepartureMonitor")) {
            goconsume = false;
        }
        
    }

    @Override
    public boolean isSnapshotAvailable(String itemName) throws SubscriptionException {
        // TODO Auto-generated method stub
        return false;
    }

    public static void onNewMessage(String key, String msg) {

        logger.info("onNewMessage: " + key + " . " + msg);

        HashMap<String, String> update = new HashMap<String, String>();
        // We have to set the key
        update.put("key", key);

        if ( keys.contains(key) ) {
            // The UPDATE command
            update.put("command", "UPDATE");
        } else {            
            // The ADD command
            update.put("command", "ADD");
            keys.add(key);
        }
        String[] tknz = msg.split("|")
        update.put("destination", tknz[0]);
        update.put("departure", tknz[1]);
        update.put("terminal", tknz[2]);
        update.put("status", tknz[3]);
        update.put("airline", tknz[4]);

        listener.update("DepartureMonitor", update, false);
    }

}