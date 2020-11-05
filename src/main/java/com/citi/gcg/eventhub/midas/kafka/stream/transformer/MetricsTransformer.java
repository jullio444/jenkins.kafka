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
package com.citi.gcg.eventhub.midas.kafka.stream.transformer;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.kstream.Window;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.citi.gcg.eventhub.midas.config.yml.KafkaStreamsConfigurationYML;
import com.citi.gcg.eventhub.midas.config.yml.OutputConfiguration;
import com.citi.gcg.eventhub.midas.constants.AppAOConstants;
import com.citi.gcg.eventhub.midas.constants.ApplicationMetricsConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/***
 * 
 * It is a kafka Transformer class which takes the data from aggregator and consolidate for the minutes window, 
 * finally the consolidate metrics will be forwarded to processor class
 * @author EventHub Dev Team
 *
 */
public class MetricsTransformer implements Transformer<Windowed<String>, JsonNode, KeyValue<String, JsonNode>> {

	private static final  Logger LOGGER = LoggerFactory.getLogger(MetricsTransformer.class);

	private ProcessorContext context;

	private KeyValueStore<String, JsonNode> metricsStateStore;

	private Window currentWindow;

	private JsonNode currentJsonAggregator;

	private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	private ZoneId headerTimeZone;
	
	private OutputConfiguration outputConfiguration;
	
	private ObjectNode output;
	
	private KafkaStreamsConfigurationYML kafkaStreamsConfigurationYML;

	private ObjectMapper objectMapper = new ObjectMapper();

	public MetricsTransformer(OutputConfiguration outputConfiguration, 
			KafkaStreamsConfigurationYML kafkaStreamsConfigurationYML) {
		this.outputConfiguration = outputConfiguration;
		this.kafkaStreamsConfigurationYML = kafkaStreamsConfigurationYML;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void init(ProcessorContext context) {
		this.context = context;
		
		output = (ObjectNode) outputConfiguration.getDailyOutputJsonObj();
		headerTimeZone = ZoneId.of(TimeZone.getTimeZone(outputConfiguration.getHeaderFormatTimeZone()).toZoneId().toString());
		
		currentWindow = new Window(0, 0) {
			@Override
			public boolean overlap(Window other) {
				return false;
			}
		};
		currentJsonAggregator = outputInitialization();		
		this.metricsStateStore = (KeyValueStore<String, JsonNode>) this.context.getStateStore(ApplicationMetricsConstants.TRANSFORMER_STATSTORE);
		this.context.schedule(Duration.ofSeconds(kafkaStreamsConfigurationYML.getWindowSizeSeconds()), 
				PunctuationType.WALL_CLOCK_TIME, currentTime -> forwardMetric());

	}

	/***
	 * It is an override method tranform of the Transformer Interface which calls for updating the transformer statestore.
	 */
	@Override
	public KeyValue<String, JsonNode> transform(Windowed<String> key, JsonNode value) {

		if (key.window().equals(currentWindow)) {
			updateStateStore(lessJson(currentJsonAggregator, value));
		} else {
			updateStateStore(value);
		}
		currentWindow = key.window();
		currentJsonAggregator = value.deepCopy();

		return null;
	}

	@Override
	public void close() {
		context.commit();
	}

	private JsonNode lessJson(JsonNode old, JsonNode current) {
		ObjectNode result = current.deepCopy();

		result.put(ApplicationMetricsConstants.AGGREGATOR_APPROVED, result.get(ApplicationMetricsConstants.AGGREGATOR_APPROVED).asInt() - old.get(ApplicationMetricsConstants.AGGREGATOR_APPROVED).asInt());
		result.put(ApplicationMetricsConstants.AGGREGATOR_PENDED, result.get(ApplicationMetricsConstants.AGGREGATOR_PENDED).asInt() - old.get(ApplicationMetricsConstants.AGGREGATOR_PENDED).asInt());
		result.put(ApplicationMetricsConstants.AGGREGATOR_DECLINED, result.get(ApplicationMetricsConstants.AGGREGATOR_DECLINED).asInt() - old.get(ApplicationMetricsConstants.AGGREGATOR_DECLINED).asInt());
		result.put(ApplicationMetricsConstants.AGGREGATOR_SUBMITTED, result.get(ApplicationMetricsConstants.AGGREGATOR_SUBMITTED).asInt() - old.get(ApplicationMetricsConstants.AGGREGATOR_SUBMITTED).asInt());
		result.put(ApplicationMetricsConstants.AGGREGATOR_SAVINGS, result.get(ApplicationMetricsConstants.AGGREGATOR_SAVINGS).asInt() - old.get(ApplicationMetricsConstants.AGGREGATOR_SAVINGS).asInt());
		result.put(ApplicationMetricsConstants.AGGREGATOR_CHECKINGS, result.get(ApplicationMetricsConstants.AGGREGATOR_CHECKINGS).asInt() - old.get(ApplicationMetricsConstants.AGGREGATOR_CHECKINGS).asInt());
		result.put(ApplicationMetricsConstants.ACCOUNT_OPENED, result.get(ApplicationMetricsConstants.ACCOUNT_OPENED).asInt() - old.get(ApplicationMetricsConstants.ACCOUNT_OPENED).asInt());
		
		return result;
	}

	/***
	 * It is a method to update the statestore with the metric valus received from aggregator
	 * @param value
	 */
	private void updateStateStore(JsonNode value) {
		ObjectNode previous = (ObjectNode) metricsStateStore.get(AppAOConstants.METRIC);
		
		if (previous == null)
			previous = (ObjectNode) outputInitialization();
        
		int approved = value.get(ApplicationMetricsConstants.AGGREGATOR_APPROVED).asInt();
		int declined = value.get(ApplicationMetricsConstants.AGGREGATOR_DECLINED).asInt();
		int pended = value.get(ApplicationMetricsConstants.AGGREGATOR_PENDED).asInt();
		int submitted = value.get(ApplicationMetricsConstants.AGGREGATOR_SUBMITTED).asInt();
		int savingAccounts = value.get(ApplicationMetricsConstants.AGGREGATOR_SAVINGS).asInt();
		int checkingAccounts =value.get(ApplicationMetricsConstants.AGGREGATOR_CHECKINGS).asInt();
		int totalAccounts = value.get(ApplicationMetricsConstants.ACCOUNT_OPENED).asInt();

		previous.put(ApplicationMetricsConstants.TOTAL_APPLICATIONS, previous.get(ApplicationMetricsConstants.TOTAL_APPLICATIONS).asInt() + submitted);
		previous.put(ApplicationMetricsConstants.TOTAL_APPROVED, previous.get(ApplicationMetricsConstants.TOTAL_APPROVED).asInt() + approved);
		previous.put(ApplicationMetricsConstants.TOTAL_PENDED, previous.get(ApplicationMetricsConstants.TOTAL_PENDED).asInt() + pended);
		previous.put(ApplicationMetricsConstants.TOTAL_DECLINED, previous.get(ApplicationMetricsConstants.TOTAL_DECLINED).asInt() + declined);
		previous.put(ApplicationMetricsConstants.TOTAL_SAVINGS, previous.get(ApplicationMetricsConstants.TOTAL_SAVINGS).asInt() + savingAccounts);
		previous.put(ApplicationMetricsConstants.TOTAL_CHECKINGS, previous.get(ApplicationMetricsConstants.TOTAL_CHECKINGS).asInt() + checkingAccounts);
		previous.put(ApplicationMetricsConstants.TOTAL_ACCOUNTS, previous.get(ApplicationMetricsConstants.TOTAL_ACCOUNTS).asInt() + totalAccounts);
		metricsStateStore.put(AppAOConstants.METRIC, previous);
	}

    /***
     * It is a Method which takes the latest metrics from state store and forwarding it to processor.
     */
	private void forwardMetric() {
		ObjectNode metrics = (ObjectNode) metricsStateStore.get(AppAOConstants.METRIC);
		if (metrics == null)
			metrics = (ObjectNode) outputInitialization();
		
		this.context.forward(AppAOConstants.METRIC, generateOutput(metrics));	

		LOGGER.debug("MetricsTransformer:forwardMetric - the consolidated data {} for the minute being sending to processor", metrics);
		metricsStateStore.put(AppAOConstants.METRIC, outputInitialization());	
	}
	
	private ObjectNode generateOutput(JsonNode value) {
		ZonedDateTime headerDate = ZonedDateTime.now(headerTimeZone);
		output.put(AppAOConstants.TRANSACTIONDATETIME, headerDate.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
		output.put(ApplicationMetricsConstants.TOTAL_APPLICATIONS, value.get(ApplicationMetricsConstants.TOTAL_APPLICATIONS).asInt());
		output.put(ApplicationMetricsConstants.TOTAL_APPROVED, value.get(ApplicationMetricsConstants.TOTAL_APPROVED).asInt());
		output.put(ApplicationMetricsConstants.TOTAL_DECLINED, value.get(ApplicationMetricsConstants.TOTAL_DECLINED).asInt());
		output.put(ApplicationMetricsConstants.TOTAL_PENDED, value.get(ApplicationMetricsConstants.TOTAL_PENDED).asInt());
		output.put(ApplicationMetricsConstants.TOTAL_ACCOUNTS, value.get(ApplicationMetricsConstants.TOTAL_ACCOUNTS).asInt());
		output.put(ApplicationMetricsConstants.TOTAL_SAVINGS, value.get(ApplicationMetricsConstants.TOTAL_SAVINGS).asInt());
		output.put(ApplicationMetricsConstants.TOTAL_CHECKINGS, value.get(ApplicationMetricsConstants.TOTAL_CHECKINGS).asInt());
		return output;
	}

	private JsonNode outputInitialization() {
		ObjectNode node = objectMapper.createObjectNode();
		node.put(ApplicationMetricsConstants.TOTAL_APPLICATIONS, 0);
		node.put(ApplicationMetricsConstants.TOTAL_APPROVED, 0);
		node.put(ApplicationMetricsConstants.TOTAL_PENDED, 0);
		node.put(ApplicationMetricsConstants.TOTAL_DECLINED, 0);
		node.put(ApplicationMetricsConstants.TOTAL_SAVINGS, 0);
		node.put(ApplicationMetricsConstants.TOTAL_CHECKINGS, 0);
		node.put(ApplicationMetricsConstants.TOTAL_ACCOUNTS, 0);
		return node;
	}

}
