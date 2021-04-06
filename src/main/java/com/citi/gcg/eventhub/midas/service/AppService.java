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

	public boolean filterSubmittedDate(String filterType,JsonNode data) {

		boolean flag= false;
		String applicationSubmittedDate= JsonTool.fetchString(data, eventPayloadConfigurationYML.getAppSubmittDatePath());
		LOGGER.info("{} Metrics evaluation: the submitted time is {} ",filterType,applicationSubmittedDate);

		if(!applicationSubmittedDate.isEmpty()) {
			
			try{

				ZonedDateTime recordDate = ZonedDateTime.parse(applicationSubmittedDate, DateTimeFormatter.ofPattern(eventPayloadConfigurationYML.getSourceTimeStampFormat()));
				boolean sameDay = ZonedDateTime.now(recordDate.getZone()).getDayOfYear() == recordDate.getDayOfYear();
				boolean sameMonth = ZonedDateTime.now(recordDate.getZone()).getMonthValue() == recordDate.getMonthValue();
				boolean sameYear = ZonedDateTime.now(recordDate.getZone()).getYear() == recordDate.getYear();

				boolean dayMetricsCondition=sameDay&&sameMonth&&sameYear;
				boolean monthMetricsCondition=sameMonth&&sameYear;

				switch(filterType) {

				case AppAOConstants.DAY_METRICTYPE: if(dayMetricsCondition) {
															flag=true;
															LOGGER.info("{} Metrics evaluation: day condition satisfied", filterType);
														}else {
															LOGGER.info("{} Metrics evaluation:: it doesn't satisfy current date", filterType);
														}
														break;

				case AppAOConstants.MONTH_METRICTYPE: if(monthMetricsCondition) {
															flag=true;
															LOGGER.info("{} Metrics evaluation: month condition satisfied", filterType);
														}else {
															LOGGER.info("{} Metrics evaluation: it doesn't satisfy current month", filterType);
														}
														break;

				case AppAOConstants.YEAR_METRICTYPE: if(sameYear) {
															flag=true;
															LOGGER.info("{} Metrics evaluation: year condition satisfied", filterType);
														}else {
															LOGGER.info("{} Metrics evaluation: it doesn't satisfy current Year", filterType);
														}
														break;
				default: LOGGER.info("{} Metrics evaluation: not satisfied with the available options", filterType);

				}

				return flag;

			}catch(Exception e) {

				LOGGER.warn("{} Metrics evaluation: An issue with parsing the applicationSubmittedDate due to invalid format with the following error {}", filterType, e.getLocalizedMessage());
				return false;

			}
		}else {
			LOGGER.warn("{} Metrics evaluation: The required element {} is not available in the payload",filterType,applicationSubmittedDate);
		}

		return flag;
	}

}
