/*
* Copyright (C) 2016 by Citigroup. All rights reserved.
* Citigroup claims copyright in this computer program as an unpublished work,
* one or more versions of which were first used to provide services to
* customers on the dates indicated in the foregoing notice. Claim of
* copyright does not imply waiver of other rights.
*
* NOTICE OF PROPRIETARY RIGHTS
*
* This program is a confidential trade secret and the property of Citigroup.
* Use, examination, reproduction, disassembly, decompiling, transfer and/or
* disclosure to others of all or any part of this software program are
* strictly prohibited except by express written agreement with Citigroup.
*/
package com.citi.gcg.eventhub.midas.kafka.stream.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/***
 * KafkaProducer class to send the final json to the derived topic
 * @author EventHub Dev
 *
 */
@Service
public class KProducer {

	private static final  Logger logger = LoggerFactory.getLogger(KProducer.class);

	
	@Autowired
	private KafkaTemplate<String,String> kafkaTemplate;
	
	
	/***
	 * Method to responsible for sending the metrics to the derived topic
	 * @param topic
	 * @param message
	 */
	public void sendMessage(String topic, String message){
		
		 ListenableFuture<SendResult<String, String>> future = 
			      kafkaTemplate.send(topic, message);
			     
			    future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
			 
			        @Override
			        public void onSuccess(SendResult<String, String> result) {
			            logger.info("Sent message {} with offset {}", message, result.getRecordMetadata().offset());
			        }
			        @Override
			        public void onFailure(Throwable ex) {
			            logger.error("Unable to send message={} due to : exception {}", message, ex.getMessage());
			            
			        }
			    });
		}
}
