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

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.citi.gcg.eventhub.midas.constants.AppYmlConfigConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/***
 * 
 * Output configuration class for fetching the required properties to create output Json
 * 
 * @author EventHub Dev Team
 *
 */
@Configuration
@ConfigurationProperties(value = AppYmlConfigConstants.OUTPUT_CONFIG_YML_PREFIX)
public class OutputConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(OutputConfiguration.class);

	private String outputFormat;
	private JsonNode dailyOutputJsonObj;
	private String outputFormatDaily;
	private JsonNode dayOutputJsonObj;
	private ObjectMapper mapper;
	private String headerFormatTimeZone;
	private String timeStampFormat;
	
	public OutputConfiguration(){
		mapper = new ObjectMapper();
	}

	@PostConstruct
	private void init() {
		try {
			dailyOutputJsonObj = mapper.readTree(outputFormat);
			dayOutputJsonObj = mapper.readTree(outputFormatDaily);

			if(LOGGER.isInfoEnabled())
				LOGGER.info(String.format("lifeTime OutputOutput format: %s", dailyOutputJsonObj.toString()));
			if(LOGGER.isInfoEnabled())
				LOGGER.info(String.format("OtherMetrics OutputOutput format: %s", dayOutputJsonObj.toString()));
		} catch (IOException exception) {
			LOGGER.error(String.format("Error Loading the JSON formats %s", exception.toString()));
		}
	}

	public String getOutputFormat() {
		return outputFormat;
	}

	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}

	public String getHeaderFormatTimeZone() {
		return headerFormatTimeZone;
	}

	public void setHeaderFormatTimeZone(String headerFormatTimeZone) {
		this.headerFormatTimeZone = headerFormatTimeZone;
	}

	public JsonNode getDailyOutputJsonObj() {
		return dailyOutputJsonObj;
	}

	public String getOutputFormatDaily() {
		return outputFormatDaily;
	}

	public void setOutputFormatDaily(String outputFormatDaily) {
		this.outputFormatDaily = outputFormatDaily;
	}

	public JsonNode getDayOutputJsonObj() {
		return dayOutputJsonObj;
	}

	public String getTimeStampFormat() {
		return timeStampFormat;
	}

	public void setTimeStampFormat(String timeStampFormat) {
		this.timeStampFormat = timeStampFormat;
	}
}