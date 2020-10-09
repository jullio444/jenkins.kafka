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

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.kstream.Window;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.state.KeyValueStore;

import com.citi.gcg.eventhub.midas.config.yml.KafkaStreamsConfigurationYML;
import com.citi.gcg.eventhub.midas.constants.ApplicationMetricsConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MetricsTransformer implements Transformer<Windowed<String>, JsonNode, KeyValue<String, JsonNode>> {

	private static final String METRICS_KEY = "metric";

	private ProcessorContext context;

	private KeyValueStore<String, JsonNode> metricsStateStore;

	private Window currentWindow;

	private JsonNode currentJsonAggregator;

	private KafkaStreamsConfigurationYML kafkaStreamsConfigurationYML;

	private ObjectMapper objectMapper = new ObjectMapper();

	public MetricsTransformer(KafkaStreamsConfigurationYML kafkaStreamsConfigurationYML) {
		this.kafkaStreamsConfigurationYML = kafkaStreamsConfigurationYML;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void init(ProcessorContext context) {
		this.context = context;
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

		return result;
	}

	private void updateStateStore(JsonNode value) {
		ObjectNode previous = (ObjectNode) metricsStateStore.get(METRICS_KEY);
		if (previous == null)
			previous = (ObjectNode) outputInitialization();

		int approved = value.get(ApplicationMetricsConstants.AGGREGATOR_APPROVED).asInt();
		int declined = value.get(ApplicationMetricsConstants.AGGREGATOR_DECLINED).asInt();
		int pended = value.get(ApplicationMetricsConstants.AGGREGATOR_PENDED).asInt();
		int submitted = value.get(ApplicationMetricsConstants.AGGREGATOR_SUBMITTED).asInt();


		previous.put(ApplicationMetricsConstants.TOTAL_APPLICATIONS, previous.get(ApplicationMetricsConstants.TOTAL_APPLICATIONS).asInt() + submitted);
		previous.put(ApplicationMetricsConstants.TOTAL_APPROVED, previous.get(ApplicationMetricsConstants.TOTAL_APPROVED).asInt() + approved);
		previous.put(ApplicationMetricsConstants.TOTAL_PENDED, previous.get(ApplicationMetricsConstants.TOTAL_PENDED).asInt() + pended);
		previous.put(ApplicationMetricsConstants.TOTAL_DECLINED, previous.get(ApplicationMetricsConstants.TOTAL_DECLINED).asInt() + declined);

		metricsStateStore.put(METRICS_KEY, previous);
	}


	private void forwardMetric() {
		ObjectNode metrics = (ObjectNode) metricsStateStore.get(METRICS_KEY);
		if (metrics == null)
			metrics = (ObjectNode) outputInitialization();
		this.context.forward(METRICS_KEY, metrics);	
		metricsStateStore.put(METRICS_KEY, outputInitialization());	
	}

	private JsonNode outputInitialization() {
		ObjectNode node = objectMapper.createObjectNode();
		node.put(ApplicationMetricsConstants.TOTAL_APPLICATIONS, 0);
		node.put(ApplicationMetricsConstants.TOTAL_APPROVED, 0);
		node.put(ApplicationMetricsConstants.TOTAL_PENDED, 0);
		node.put(ApplicationMetricsConstants.TOTAL_DECLINED, 0);
		return node;
	}

}
