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
package com.citi.gcg.eventhub.midas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.citi.gcg.eventhub.midas.kafka.stream.processor.KafkaProcesor;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class AppService {

	private static final  Logger LOGGER = LoggerFactory.getLogger(AppService.class);
	public boolean filterEvents(JsonNode filters, JsonNode data) {
		ResultsExtractor resultsExtractor = new ResultsExtractor();
		if((filters != null) && (filters.size() > 0)) {
			return resultsExtractor.filterMatch(data, filters);
		}
		else {
			LOGGER.warn("AppService:filterEvents- there are no filters available" );
		}
		return Boolean.TRUE;
	}

}
