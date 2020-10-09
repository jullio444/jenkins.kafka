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
import com.citi.gcg.eventhub.midas.constants.ResultsExtractorConstants;
import com.citi.gcg.eventhub.midas.service.ResultsExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ApplicationMetricsAggregator implements Aggregator<String, JsonNode, JsonNode> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationMetricsAggregator.class);

	private EventPayloadConfigurationYML eventPayloadConfigurationYML;

	public ApplicationMetricsAggregator(EventPayloadConfigurationYML eventPayloadConfigurationYML) {
		this.eventPayloadConfigurationYML = eventPayloadConfigurationYML;
	}

	@Override
	public JsonNode apply(String key, JsonNode value, JsonNode aggregate) {

		ObjectNode objectNode = (ObjectNode) aggregate;

		ResultsExtractor resultsExtractor= new ResultsExtractor();

		Map<String, String> typeStatus = resultsExtractor.extractResultsFromData(value, eventPayloadConfigurationYML.getConditions());
		System.err.println("Type Status: "+typeStatus);

		if(!typeStatus.isEmpty()) {

			String currentApplicationStatus = typeStatus.get(ApplicationMetricsConstants.CURRENT_APPLICATION_STATUS);
			LOGGER.info("ApplicationMetricsAggregator - apply: Current Application Status Recieved: {}", currentApplicationStatus);

			String previousApplicationStatus = typeStatus.get(ApplicationMetricsConstants.PREVIOUS_APPLICATION_STATUS);
			LOGGER.debug("ApplicationMetricsAggregator - apply: Previous Application Status Recieved: {}", currentApplicationStatus);

			String applicationOperation = typeStatus.get(ApplicationMetricsConstants.APPLICATION_OPERATION);
			LOGGER.debug("ApplicationMetricsAggregator - apply: Application Operation: {}", applicationOperation);

			if(applicationOperation.equals(ApplicationMetricsConstants.APPLICATION_OPERATION_NEW)) {
				objectNode.put(ApplicationMetricsConstants.AGGREGATOR_SUBMITTED, objectNode.get(ApplicationMetricsConstants.AGGREGATOR_SUBMITTED).asInt() + 1);
			}
			if((currentApplicationStatus!=null )
					&& (previousApplicationStatus!=null)) {
				if(!currentApplicationStatus.equals(previousApplicationStatus)) {
					objectNode.put(currentApplicationStatus, objectNode.get(currentApplicationStatus).asInt() + 1);
					if(!applicationOperation.equals(ApplicationMetricsConstants.APPLICATION_OPERATION_NEW)) {
						objectNode.put(previousApplicationStatus, objectNode.get(previousApplicationStatus).asInt(0) - 1);	
					}
						 
				}
			}


		}


		return aggregate;
	}

}