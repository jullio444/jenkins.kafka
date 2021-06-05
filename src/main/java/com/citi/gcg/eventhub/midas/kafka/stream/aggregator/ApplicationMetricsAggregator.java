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
package com.citi.gcg.eventhub.midas.kafka.stream.aggregator;

import java.util.Map;

import org.apache.kafka.streams.kstream.Aggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.citi.gcg.eventhub.midas.config.yml.EventPayloadConfigurationYML;
import com.citi.gcg.eventhub.midas.constants.ApplicationMetricsConstants;
import com.citi.gcg.eventhub.midas.service.ResultsExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/***
 * A Custom aggregator class to do aggregation on the payload and send back the result to transformer
 * 
 *
 *
 */
public class ApplicationMetricsAggregator implements Aggregator<String, JsonNode, JsonNode> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationMetricsAggregator.class);

	private EventPayloadConfigurationYML eventPayloadConfigurationYML;
	public ApplicationMetricsAggregator(EventPayloadConfigurationYML eventPayloadConfigurationYML) {
		this.eventPayloadConfigurationYML = eventPayloadConfigurationYML;
	}

	/***
	 * It is to override the apply method of aggregator Interface to take the data from input kafka streams 
	 * after filtering to do required aggregations
	 * @return JsonNode
	 */
	@Override
	public JsonNode apply(String key, JsonNode value, JsonNode aggregate) {
		
		ObjectNode objectNode = (ObjectNode) aggregate;

		ResultsExtractor resultsExtractor= new ResultsExtractor();

		Map<String, String> typeStatus = resultsExtractor.extractResultsFromData(value, eventPayloadConfigurationYML.getConditions());
		LOGGER.info("ApplicationMetricsAggregator - apply: result from evaluation : {}", typeStatus);

		if(!typeStatus.isEmpty()) {

			String currentApplicationStatus = typeStatus.get(ApplicationMetricsConstants.CURRENT_APPLICATION_STATUS);
			LOGGER.debug("ApplicationMetricsAggregator - apply: Current Application Status Recieved: {}", currentApplicationStatus);

			String previousApplicationStatus = typeStatus.get(ApplicationMetricsConstants.PREVIOUS_APPLICATION_STATUS);
			LOGGER.debug("ApplicationMetricsAggregator - apply: Previous Application Status Recieved: {}", currentApplicationStatus);

			String applicationOperation = typeStatus.get(ApplicationMetricsConstants.APPLICATION_OPERATION);
			LOGGER.debug("ApplicationMetricsAggregator - apply: Application Operation: {}", applicationOperation);

			String accountOpened= typeStatus.get(ApplicationMetricsConstants.ACCOUNT_STATUS);
			LOGGER.debug("ApplicationMetricsAggregator - apply: accountOpened status: {}", accountOpened);

			String typeOfAccount = typeStatus.get(ApplicationMetricsConstants.ACCOUNT_TYPE);
			LOGGER.debug("ApplicationMetricsAggregator - apply: Type of account : {}", typeOfAccount);

			applicationMetrics(applicationOperation,currentApplicationStatus,previousApplicationStatus,objectNode);
			   
			if(accountOpened!=null) {

				objectNode.put(accountOpened, objectNode.get(accountOpened).asInt()+1);

				objectNode.put(typeOfAccount, objectNode.get(typeOfAccount).asInt()+1);
			}

		}

		return aggregate;
	}

	/***
	 * It is to insert the required application metrics depends on the status details received from payload evaluation
	 * @param applicationOperation
	 * @param currentApplicationStatus
	 * @param previousApplicationStatus
	 * @param objectNode
	 * @return objectNode
	 */
	private ObjectNode applicationMetrics(String applicationOperation, String currentApplicationStatus,
			String previousApplicationStatus, ObjectNode objectNode) {
		
		if(applicationOperation.equals(ApplicationMetricsConstants.APPLICATION_OPERATION_NEW)) {
			objectNode.put(ApplicationMetricsConstants.AGGREGATOR_SUBMITTED, objectNode.get(ApplicationMetricsConstants.AGGREGATOR_SUBMITTED).asInt() + 1);
			objectNode.put(currentApplicationStatus, objectNode.get(currentApplicationStatus).asInt() + 1);
		}
		else if((currentApplicationStatus!=null ) && (previousApplicationStatus!=null) && (!currentApplicationStatus.equals(previousApplicationStatus))) {
			
			objectNode.put(currentApplicationStatus, objectNode.get(currentApplicationStatus).asInt() + 1);
			objectNode.put(previousApplicationStatus, objectNode.get(previousApplicationStatus).asInt(0) - 1);	
		}
		
		return objectNode;
	}

}