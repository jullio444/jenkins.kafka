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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.citi.gcg.eventhub.midas.constants.ResultsExtractorConstants;
import com.citi.gcg.eventhub.midas.exception.MetricsApplicationRuntimeException;
import com.fasterxml.jackson.databind.JsonNode;

/***
 * 
 * It is payload evaluation class where it checks the filters and conditions against the payload,
 * finally provides the filteredout and application status details
 * @author EventHub Dev Team
 *
 */
@Service
public class ResultsExtractor {

	private static final  Logger LOGGER = LoggerFactory.getLogger(ResultsExtractor.class);
	
	public Map<String, String> extractResultsFromData(JsonNode data, JsonNode conditionInput){
		return processForExtractResultsFromData(data, conditionInput);
	}

	public boolean filterMatch(JsonNode data, JsonNode filters) {
		
		
		List<Boolean> successFlags = new ArrayList<>();
		
		JsonNode conditionsArray = filters.get(ResultsExtractorConstants.CONST_CONDITIONS);
		String filterType = filters.has(ResultsExtractorConstants.CONST_FILTER_TYPE) ? filters.get(ResultsExtractorConstants.CONST_FILTER_TYPE).asText() : ResultsExtractorConstants.CONDITION_ANY;

		for(int conditionIndex = 0; conditionIndex < conditionsArray.size(); conditionIndex++) {
			JsonNode condition = conditionsArray.get(conditionIndex);

			String[] field = condition.get(ResultsExtractorConstants.CONST_FIELD).asText().split(ResultsExtractorConstants.DOT);
			checkCondition(successFlags, 
					field, 0, 
					condition.get(ResultsExtractorConstants.CONST_VALUE).asText(),
					data,
					filterType);
		}
		

		if((successFlags.size()) > 0 && (!successFlags.contains(Boolean.FALSE))) {
			
			LOGGER.debug("ResultsExtractor: the filter condition is matched with the payload ");
			return Boolean.TRUE;
		}

		return Boolean.FALSE;

	}

	private Map<String, String> processForExtractResultsFromData(JsonNode dataJsonObject,
			JsonNode conditionsJsonObject) {

		Map<String, String> result = new HashMap<>();

		conditionsJsonObject.fieldNames().forEachRemaining(resultantKey -> {
			if(!result.containsKey(resultantKey)) {
				String value = extractResultValue(conditionsJsonObject.get(resultantKey), dataJsonObject);
				result.put(resultantKey, value);
			}
		}); 

		return result;
	}

	private String extractResultValue(JsonNode allConditionJsonObject, JsonNode dataJsonObject) {

		String result  = ResultsExtractorConstants.STRING_NULL;
		Iterator<String> keys = allConditionJsonObject.fieldNames();
		while(keys.hasNext()){

			String resultValue = keys.next();

			JsonNode conditionJsonObject = allConditionJsonObject.get(resultValue);
			JsonNode conditionsArray = conditionJsonObject.get(ResultsExtractorConstants.CONST_CONDITIONS);
			int conditionsCount = conditionsArray.size();

			String filterType = conditionJsonObject.has(ResultsExtractorConstants.CONST_FILTER_TYPE) ? conditionJsonObject.get(ResultsExtractorConstants.CONST_FILTER_TYPE).asText() : ResultsExtractorConstants.CONDITION_ANY;

			int conditionSuccessCount = conditionSuccessCount(conditionsArray, dataJsonObject, filterType);
			if(filterType.equalsIgnoreCase(ResultsExtractorConstants.CONDITION_ALL)) {
				if(conditionsCount == conditionSuccessCount) {
					result = resultValue;
					break;
				}
			}else if(filterType.equalsIgnoreCase(ResultsExtractorConstants.CONDITION_ANY)) {
				if(conditionSuccessCount >= ResultsExtractorConstants.CONST_ONE) {
					result = resultValue;
					break;
				}
			}
		}

		return result;
	}

	private int conditionSuccessCount(JsonNode conditionsArray, JsonNode dataJsonObject, String filterType) {

		int successCount = 0;

		for(int conditionIndex = 0; conditionIndex < conditionsArray.size(); conditionIndex++) {
			JsonNode condition = conditionsArray.get(conditionIndex);
			List<Boolean> successFlags = new ArrayList<>();

			String[] field = condition.get(ResultsExtractorConstants.CONST_FIELD).asText().split(ResultsExtractorConstants.DOT);
			checkCondition(successFlags, 
					field, 0, 
					condition.get(ResultsExtractorConstants.CONST_VALUE).asText(),
					dataJsonObject,
					filterType);
			if((successFlags.size()) > 0 && (!successFlags.contains(Boolean.FALSE))) {
				successCount ++;
			}
		}

		return successCount;
	}

	private List<Boolean> checkCondition(List<Boolean> successFlags, String[] field, int fieldIndex, String value, JsonNode dataJsonObject, String filterType){

		if(dataJsonObject.isContainerNode()) {
			if(fieldIndex >= field.length) {
				throw new MetricsApplicationRuntimeException("Reached maximum fields. Improper condition.");
			}else{
				String key = field[fieldIndex];
				if(dataJsonObject.has(key) && (!StringUtils.isNumeric(key)) && (!key.equals(ResultsExtractorConstants.STAR))) {
					fieldIndex++;
					checkCondition(successFlags, field, fieldIndex, value, dataJsonObject.get(key), filterType);
				}else {
					successFlags.add(Boolean.FALSE);
				}
			}

		} if(dataJsonObject.isArray()) {
			if(fieldIndex >= field.length) {
				throw new MetricsApplicationRuntimeException("Reached maximum fields. Improper condition.");
			}else {
				String key = field[fieldIndex];
				if(StringUtils.isNumeric(key)) {
					fieldIndex++;
					int arrayIndex = Integer.parseInt(key);
					checkCondition(successFlags, field, fieldIndex, value, dataJsonObject.get(arrayIndex), filterType);
				}else if(key.equals(ResultsExtractorConstants.STAR)) {
					fieldIndex++;
					for(int arrayIndex = 0; arrayIndex < dataJsonObject.size(); arrayIndex++) {
						checkCondition(successFlags, field, fieldIndex, value, dataJsonObject.get(arrayIndex), filterType);
					}
				}else {
					checkCondition(successFlags, field, fieldIndex, value, dataJsonObject.get(ResultsExtractorConstants.DEFAULT_ARRAY_GET), filterType);
				}
			}
		}else if(dataJsonObject.isTextual()){
			String dataValue = dataJsonObject.asText(ResultsExtractorConstants.STRING_NULL);
			if(dataValue.matches(value)) {
				
				LOGGER.debug("ResultsExtractor: the expected value {} matches with the value {} in payload ",
						dataValue, value);
				
				successFlags.add(Boolean.TRUE);
			}else {
				if(!filterType.equalsIgnoreCase(ResultsExtractorConstants.CONDITION_ANY)) {
					successFlags.add(Boolean.FALSE);
				}
			}
		}
		return successFlags;
	}

}
