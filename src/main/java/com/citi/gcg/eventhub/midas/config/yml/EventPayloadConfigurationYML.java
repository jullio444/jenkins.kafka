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
package com.citi.gcg.eventhub.midas.config.yml;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.citi.gcg.eventhub.midas.constants.AppYmlConfigConstants;
import com.citi.gcg.eventhub.midas.exception.MetricsApplicationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/***
 * EventPayaload configuration class for fetching the required conditions Json for evaluating the payload
 * 
 * @author EventHub Dev Team
 *
 */
@Configuration
@ConfigurationProperties(value=AppYmlConfigConstants.EVENT_PAYLOAD_YML_PREFIX)
public class EventPayloadConfigurationYML {

	private static final  Logger LOGGER = LoggerFactory.getLogger(EventPayloadConfigurationYML.class);

	private String categorization;

	private JsonNode filters;

	private JsonNode conditions;

	public EventPayloadConfigurationYML() {
		//Default constructor
	}

	public String getCategorization() {
		return categorization;
	}

	/***
	 *  This method to fetch the conditions Json file from configuration YML 
	 *  and divide its filters and conditions separately which will be used for payload evaluation
	 *  
	 * @param categorization
	 */
	public void setCategorization(String categorization) {
		this.categorization = categorization;

		ObjectMapper mapper = new ObjectMapper();

		JsonNode innerFilters = null;
		JsonNode innerconditions = null;

		if(categorization.startsWith(AppYmlConfigConstants.FILE_PREFIX)) {
			try {

				LOGGER.trace("EventPayloadConfigurationYML: loading of the conditions JSON file {} ",categorization);

				categorization = new String(Files.readAllBytes(Paths.get(categorization.substring(AppYmlConfigConstants.FILE_PREFIX.length()))));
				JsonNode jsonObj = mapper.readTree(categorization);
				if(jsonObj.has(AppYmlConfigConstants.CONST_FILTERS)) {
					innerFilters = jsonObj.get(AppYmlConfigConstants.CONST_FILTERS);
				}else {
					LOGGER.info("EventPayloadConfigurationYML: there are no filters provided in the conditions JSON");
				}

				if(jsonObj.has(AppYmlConfigConstants.CONST_CONDITIONS)) {
					innerconditions = jsonObj.get(AppYmlConfigConstants.CONST_CONDITIONS);
				}else {
					throw new MetricsApplicationException("Conditions not provided for metrics.");
				}

			} catch (Exception e) {
				LOGGER.error("EventPayloadConfigurationYML: unable to parse the JSON due to the exception {}", e.getLocalizedMessage());
			}
		}else {

			LOGGER.warn("EventPayloadConfigurationYML: the prefix of conditions JSON file doesn't match with {} ,hence provide the correct configuration", AppYmlConfigConstants.FILE_PREFIX);
		}

		setFilters(innerFilters);
		setConditions(innerconditions);
	}

	public JsonNode getFilters() {
		return filters;
	}

	public void setFilters(JsonNode filters) {
		this.filters = filters;
	}

	public JsonNode getConditions() {
		return conditions;
	}

	public void setConditions(JsonNode conditions) {
		this.conditions = conditions;
	}

}
