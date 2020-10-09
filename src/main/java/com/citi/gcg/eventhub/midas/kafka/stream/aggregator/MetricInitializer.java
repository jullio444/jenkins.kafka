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

import org.apache.kafka.streams.kstream.Initializer;

import com.citi.gcg.eventhub.midas.constants.ApplicationMetricsConstants;
import com.citi.gcg.eventhub.midas.constants.ResultsExtractorConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MetricInitializer implements  Initializer<JsonNode>{

	@Override
	public JsonNode apply() {
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectNode node = objectMapper.createObjectNode();
		node.put(ApplicationMetricsConstants.AGGREGATOR_APPROVED, 0);
		node.put(ApplicationMetricsConstants.AGGREGATOR_DECLINED, 0);
		node.put(ApplicationMetricsConstants.AGGREGATOR_PENDED, 0);
		node.put(ApplicationMetricsConstants.AGGREGATOR_SUBMITTED, 0);
		node.put(ResultsExtractorConstants.STRING_NULL, 0);
		return node;
	}
}
