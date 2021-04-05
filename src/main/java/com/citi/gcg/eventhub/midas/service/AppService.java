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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.citi.gcg.eventhub.midas.config.yml.EventPayloadConfigurationYML;
import com.citi.gcg.eventhub.midas.constants.AppAOConstants;
import com.citi.gcg.eventhub.midas.constants.ResultsExtractorConstants;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class AppService {
	
	@Autowired
	EventPayloadConfigurationYML eventPayloadConfigurationYML;

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

	public boolean filterSubmittedDate(String filterType,JsonNode data, String appSubmittedDatePath) {
		
		boolean flag= false;
		String applicationSubmittedDate= JsonTool.fetchString(data, eventPayloadConfigurationYML.getAppSubmittDatePath());
		LOGGER.info("the submitted time is {} ",applicationSubmittedDate);
		
		if(applicationSubmittedDate!=null&&applicationSubmittedDate!=ResultsExtractorConstants.STRING_EMPTY) {
			ZonedDateTime recordDate = ZonedDateTime.parse(applicationSubmittedDate, DateTimeFormatter.ofPattern(eventPayloadConfigurationYML.getSourceTimeStampFormat()));

			boolean sameDay = ZonedDateTime.now(recordDate.getZone()).getDayOfYear() == recordDate.getDayOfYear();
			boolean sameMonth = ZonedDateTime.now(recordDate.getZone()).getMonthValue() == recordDate.getMonthValue();
			boolean sameYear = ZonedDateTime.now(recordDate.getZone()).getYear() == recordDate.getYear();

			switch(filterType) {

			case AppAOConstants.DAY_METRICTYPE: if(sameDay&&sameMonth&&sameYear==true) {
													  flag=true;
													  LOGGER.info("day condition satisfied");
												   }else {
													  LOGGER.info("it doesn't satisfy current date");
												}
												break;

			case AppAOConstants.MONTH_METRICTYPE: if(sameMonth&&sameYear==true) {
														flag=true;
														LOGGER.info("month condition satisfied");
													}else {
														LOGGER.info("it doesn't satisfy current month");
													}
													break;

			case AppAOConstants.YEAR_METRICTYPE: if(sameYear==true) {
														flag=true;
														LOGGER.info("year condition satisfied");
												}else {
														LOGGER.info("it doesn't satisfy current Year");
												}
												break;
			default: LOGGER.info("not satisfied with the available options");

			}
			
		}else {
			LOGGER.warn("The required element {} is not available in the payload",applicationSubmittedDate);
		}
		return flag;
	}

}
