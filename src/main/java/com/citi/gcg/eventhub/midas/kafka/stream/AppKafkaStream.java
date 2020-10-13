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
package com.citi.gcg.eventhub.midas.kafka.stream;

import java.time.Duration;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.WindowBytesStoreSupplier;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binder.kafka.streams.annotations.KafkaStreamsStateStore;
import org.springframework.cloud.stream.binder.kafka.streams.properties.KafkaStreamsStateStoreProperties.StoreType;
import org.springframework.integration.config.EnableIntegration;

import com.citi.gcg.eventhub.midas.config.yml.EventPayloadConfigurationYML;
import com.citi.gcg.eventhub.midas.config.yml.KafkaStreamsConfigurationYML;
import com.citi.gcg.eventhub.midas.config.yml.OutputConfiguration;
import com.citi.gcg.eventhub.midas.constants.ApplicationMetricsConstants;
import com.citi.gcg.eventhub.midas.kafka.stream.aggregator.ApplicationMetricsAggregator;
import com.citi.gcg.eventhub.midas.kafka.stream.aggregator.MetricInitializer;
import com.citi.gcg.eventhub.midas.kafka.stream.processor.KProducer;
import com.citi.gcg.eventhub.midas.kafka.stream.processor.KafkaProcesor;
import com.citi.gcg.eventhub.midas.kafka.stream.transformer.MetricsTransformer;
import com.citi.gcg.eventhub.midas.service.AppService;
import com.fasterxml.jackson.databind.JsonNode;

/***
 * Kafka Streams class that read the data from input topic 
 * and does filtering, aggregation, transformation and processing to create the required output Json
 * 
 * @author EventHub Dev Team
 *
 */
@EnableBinding(KafkaEventProcessor.class)
@EnableIntegration
@EnableAutoConfiguration
public class AppKafkaStream {

	private static final  Logger LOGGER = LoggerFactory.getLogger(AppKafkaStream.class);

	@Autowired
	private AppService appService;
	
	@Autowired
	private EventPayloadConfigurationYML eventPayloadConfigurationYML;
	
	@Autowired
	private KafkaStreamsConfigurationYML kafkaStreamsConfigurationYML;
	
	@Autowired
	private KProducer kProducer;
	
	@Autowired
	private OutputConfiguration outputConfiguration;

	public AppKafkaStream(AppService appService, EventPayloadConfigurationYML eventPayloadConfigurationYML,
			KafkaStreamsConfigurationYML kafkaStreamsConfigurationYML,
			OutputConfiguration outputConfiguration) {
		super();
		this.appService = appService;
		this.eventPayloadConfigurationYML = eventPayloadConfigurationYML;
		this.kafkaStreamsConfigurationYML = kafkaStreamsConfigurationYML;
		this.outputConfiguration = outputConfiguration;
	}

	@KafkaStreamsStateStore(name = ApplicationMetricsConstants.TRANSFORMER_STATSTORE, type = StoreType.KEYVALUE, keySerde = ApplicationMetricsConstants.KEY_SERDE, valueSerde = ApplicationMetricsConstants.VALUE_SERDE)
	@StreamListener(ApplicationMetricsConstants.INPUT_TOPIC)
	public void proccess(KStream<String, JsonNode> stream) {
		stream
		.filter((key, value) -> appService.filterEvents(eventPayloadConfigurationYML.getFilters(), value))
		.selectKey((k,v) -> k = filtertNullKey(k))
		.groupByKey()
		.windowedBy(TimeWindows.of(Duration.ofSeconds(kafkaStreamsConfigurationYML.getWindowSizeSeconds())))
		.aggregate(new MetricInitializer(), 
				new ApplicationMetricsAggregator(eventPayloadConfigurationYML),
				materialized(ApplicationMetricsConstants.AGGREGATOR_STATSTORE))
		.toStream()
		.transform(() -> new MetricsTransformer(kafkaStreamsConfigurationYML), ApplicationMetricsConstants.TRANSFORMER_STATSTORE)
		.process(() -> new KafkaProcesor(outputConfiguration,kafkaStreamsConfigurationYML,kProducer));
	}
	
	private String filtertNullKey(String k) {
		LOGGER.trace("AppKafkaStream:filtertNullKey - Handling the null key scenario");
		return k == null ? "EH-Aggregation" : k;
	}

	protected WindowBytesStoreSupplier getSupplier(String stateStoreName) {
		
		LOGGER.info("AppKafkaStream:getSupplier - Configuring statestore {} with retention {} and window size {}",
				stateStoreName,kafkaStreamsConfigurationYML.getCleanUpPolicy(),kafkaStreamsConfigurationYML.getWindowSizeSeconds());
		return Stores.persistentWindowStore(stateStoreName, 
				Duration.ofDays(kafkaStreamsConfigurationYML.getCleanUpPolicy()), 
				Duration.ofSeconds(kafkaStreamsConfigurationYML.getWindowSizeSeconds()), Boolean.FALSE);
	}

	protected Materialized<String, JsonNode, WindowStore<Bytes, byte[]>> materialized(String stateStoreName) {
		
		LOGGER.info("AppKafkaStream:materialized - Configuring statestore {} with KeySerde as StringSerde and ValueSerde as JSONSerde",
				stateStoreName);
		
		Materialized<String, JsonNode, WindowStore<Bytes, byte[]>> stateStoreWindowed = Materialized
				.as(getSupplier(stateStoreName));
		stateStoreWindowed.withKeySerde(Serdes.String());
		stateStoreWindowed.withValueSerde(Serdes.serdeFrom(new JsonSerializer(), new JsonDeserializer()));
		return stateStoreWindowed;
	}
	
}
