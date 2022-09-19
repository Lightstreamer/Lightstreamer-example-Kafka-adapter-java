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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static List<String> keys = new ArrayList<String>();

    private boolean currtime = false;

    private static ConsumerLoop consumer;

    private String kconnstring = "b-2.democluster1.rw4f0s.c9.kafka.eu-west-1.amazonaws.com:9092,b-1.democluster1.rw4f0s.c9.kafka.eu-west-1.amazonaws.com:9092";

    private String kconsumergroupid;

    private String ktopicname;

    private static final String KBOOTSTRAPSERVERS = "kafka_bootstrap_servers";

    private static final String KCONSGROUPID = "kafka_consumer_group_id";

    private static final String KTOPIC = "kafka_topic_name";


    @Override
    public void init(Map params, File configDir) throws DataProviderException {

        logger.info("Start initialization of the kafkademo Data adapter ... ");

        if (params.containsKey(KBOOTSTRAPSERVERS)) {
            logger.info("kafka boostrap servers configured: " + params.get(KBOOTSTRAPSERVERS));
            this.kconnstring = (String) params.get(KBOOTSTRAPSERVERS);
        }

        if (params.containsKey(KCONSGROUPID)) {
            logger.info("kafka consumer group.id configured: " + params.get(KCONSGROUPID));
            this.kconsumergroupid = (String) params.get(KCONSGROUPID);
        }

        if (params.containsKey(KTOPIC)) {
            logger.info("kafka topic name configured: " + params.get(KTOPIC));
            this.ktopicname = (String) params.get(KTOPIC);
        }

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
            consumer = new ConsumerLoop(this, kconnstring, kconsumergroupid, ktopicname);
            consumer.start();
        } else if (itemName.startsWith("CurrTime")) {
            currtime = true;
        } else {
            logger.warn("Requested item not expected.");
        }
        
    }

    @Override
    public void unsubscribe(String itemName) throws SubscriptionException, FailureException {

        if (itemName.startsWith("DepartureMonitor")) {
            consumer.stopconsuming();
        } else if (itemName.startsWith("CurrTime")) {
            currtime = false;
        }
    }

    @Override
    public boolean isSnapshotAvailable(String itemName) throws SubscriptionException {
        // TODO Auto-generated method stub
        return false;
    }

    public static void onNewMessage(String key, String msg) {
        
        logger.info("onNewMessage: " +  key + " . " + msg);

        HashMap<String, String> update = new HashMap<String, String>();
        // We have to set the key
        update.put("key",  key);

        String[] tknz = msg.split("\\|");

        if ( keys.contains( key) ) {
            if (tknz[3].equalsIgnoreCase("Deleted")) {
                // delete
                logger.info("delete");
                update.put("command", "DELETE");
                keys.remove(key);
            } else {
                // UPDATE command
                logger.info("update");
                update.put("command", "UPDATE");
            }
        } else {            
            // ADD command
            logger.info("add");
            update.put("command", "ADD");
            keys.add(key);
        }

        update.put("destination", tknz[0]);
        update.put("departure", tknz[1]);
        update.put("terminal", tknz[2]);
        update.put("status", tknz[3]);
        update.put("airline", tknz[4]);

        listener.update("DepartureMonitor", update, false);
    }

    public void onCurrentTimeUpdate(String time) {
        
        logger.info("onCurrentTimeUpdate: " + time);

        if ( this.currtime) {
            HashMap<String, String> update = new HashMap<String, String>();
            // We have to set the key
            update.put("time",  time);

            listener.update("CurrTime", update, false);
        }

    }

}