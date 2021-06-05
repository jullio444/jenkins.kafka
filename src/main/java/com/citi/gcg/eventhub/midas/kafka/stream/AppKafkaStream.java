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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.WindowBytesStoreSupplier;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

import com.citi.gcg.eventhub.midas.config.yml.EventPayloadConfigurationYML;
import com.citi.gcg.eventhub.midas.config.yml.KafkaStreamsConfigurationYML;
import com.citi.gcg.eventhub.midas.config.yml.OutputConfiguration;
import com.citi.gcg.eventhub.midas.constants.AppAOConstants;
import com.citi.gcg.eventhub.midas.constants.ApplicationMetricsConstants;
import com.citi.gcg.eventhub.midas.kafka.serde.JsonSerde;
import com.citi.gcg.eventhub.midas.kafka.stream.aggregator.ApplicationMetricsAggregator;
import com.citi.gcg.eventhub.midas.kafka.stream.aggregator.MetricInitializer;
import com.citi.gcg.eventhub.midas.kafka.stream.transformer.MetricsTransformer;
import com.citi.gcg.eventhub.midas.service.AppService;
import com.citi.gcg.eventhub.midas.service.JsonTool;
import com.fasterxml.jackson.databind.JsonNode;

/***
 * Kafka Streams class that read the data from input topic 
 * and does filtering, aggregation, transformation and processing to create the required output Json
 * 
 *
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
	private OutputConfiguration outputConfiguration;

	@Autowired
	private ApplicationContext applicationContext;

	public AppKafkaStream(AppService appService, EventPayloadConfigurationYML eventPayloadConfigurationYML,
			KafkaStreamsConfigurationYML kafkaStreamsConfigurationYML,
			OutputConfiguration outputConfiguration) {
		super();
		this.appService = appService;
		this.eventPayloadConfigurationYML = eventPayloadConfigurationYML;
		this.kafkaStreamsConfigurationYML = kafkaStreamsConfigurationYML;
		this.outputConfiguration = outputConfiguration;
	}


	/***
	 * It is the method responsible for consuming the events from input topic and 
	 * required processing 
	 * @param stream
	 */



	@StreamListener(ApplicationMetricsConstants.INPUT_TOPIC)
	public void proccess(KStream<String, JsonNode> stream) {

		StoreBuilder<KeyValueStore<String, JsonNode>> storeBuilder1 = Stores.keyValueStoreBuilder(
				Stores.persistentKeyValueStore(ApplicationMetricsConstants.DAY_TRANSFORMER_STATSTORE),
				Serdes.String(),
				new JsonSerde()
				);
		StoreBuilder<KeyValueStore<String, JsonNode>> storeBuilder2 = Stores.keyValueStoreBuilder(
				Stores.persistentKeyValueStore(ApplicationMetricsConstants.MONTH_TRANSFORMER_STATSTORE),
				Serdes.String(),
				new JsonSerde());
		StoreBuilder<KeyValueStore<String, JsonNode>> storeBuilder3 = Stores.keyValueStoreBuilder(
				Stores.persistentKeyValueStore(ApplicationMetricsConstants.YEAR_TRANSFORMER_STATSTORE),
				Serdes.String(),
				new JsonSerde());

		StoreBuilder<KeyValueStore<String, JsonNode>> storeBuilder4 = Stores.keyValueStoreBuilder(
				Stores.persistentKeyValueStore(ApplicationMetricsConstants.TRANSFORMER_STATSTORE),
				Serdes.String(),
				new JsonSerde()
				);

		try {
			applicationContext.getBean(StreamsBuilderFactoryBean.class).getObject().addStateStore(storeBuilder1);
			applicationContext.getBean(StreamsBuilderFactoryBean.class).getObject().addStateStore(storeBuilder2);
			applicationContext.getBean(StreamsBuilderFactoryBean.class).getObject().addStateStore(storeBuilder3);
			applicationContext.getBean(StreamsBuilderFactoryBean.class).getObject().addStateStore(storeBuilder4);

		} catch (Exception e) {
			LOGGER.error("Can not find 'stream-builder-process' bean.");
		}


		/***LifeTime*****/

		stream
		.filter((k,v)-> v!=null)
		.filter((key, value) -> appService.filterEvents(eventPayloadConfigurationYML.getFilters(), value))
		.filter((key, value) -> validateSubmittedDate(value))
		.selectKey((k,v) -> k = filtertNullKey())
		.groupByKey()
		.windowedBy(TimeWindows.of(Duration.ofSeconds(kafkaStreamsConfigurationYML.getWindowSizeSeconds())))
		.aggregate(new MetricInitializer(), 
				new ApplicationMetricsAggregator(eventPayloadConfigurationYML),
				materialized(ApplicationMetricsConstants.AGGREGATOR_STATSTORE))
		.toStream()
		.transform(() -> new MetricsTransformer(outputConfiguration,
				kafkaStreamsConfigurationYML, ApplicationMetricsConstants.TRANSFORMER_STATSTORE, AppAOConstants.LIFETIME_METRICTYPE), 
				ApplicationMetricsConstants.TRANSFORMER_STATSTORE)

		.to(kafkaStreamsConfigurationYML.getOutputTopic());


		/*****Day****/

		stream
		.filter((k,v)-> v!=null)
		.filter((key, value) -> appService.filterEvents(eventPayloadConfigurationYML.getFilters(), value))
		.filter((k,v)-> appService.filterSubmittedDate(AppAOConstants.DAY_METRICTYPE, v))
		.selectKey((k,v) -> k = filtertNullKey())
		.groupByKey()
		.windowedBy(TimeWindows.of(Duration.ofSeconds(kafkaStreamsConfigurationYML.getWindowSizeSeconds())))
		.aggregate(new MetricInitializer(), 
				new ApplicationMetricsAggregator(eventPayloadConfigurationYML),
				materialized(ApplicationMetricsConstants.DAY_AGGREGATOR_STATSTORE))
		.toStream()
		.transform(() -> new MetricsTransformer(outputConfiguration,
				kafkaStreamsConfigurationYML,ApplicationMetricsConstants.DAY_TRANSFORMER_STATSTORE,AppAOConstants.DAY_METRICTYPE), 
				ApplicationMetricsConstants.DAY_TRANSFORMER_STATSTORE)

		.to(kafkaStreamsConfigurationYML.getOutputTopic());


		/****Month****/

		stream
		.filter((k,v)-> v!=null)
		.filter((key, value) -> appService.filterEvents(eventPayloadConfigurationYML.getFilters(), value))
		.filter((k,v)-> appService.filterSubmittedDate(AppAOConstants.MONTH_METRICTYPE, v))
		.selectKey((k,v) -> k = filtertNullKey())
		.groupByKey()
		.windowedBy(TimeWindows.of(Duration.ofSeconds(kafkaStreamsConfigurationYML.getWindowSizeSeconds())))
		.aggregate(new MetricInitializer(), 
				new ApplicationMetricsAggregator(eventPayloadConfigurationYML),
				materialized(ApplicationMetricsConstants.MONTH_AGGREGATOR_STATSTORE))
		.toStream()
		.transform(() -> new MetricsTransformer(outputConfiguration,
				kafkaStreamsConfigurationYML,ApplicationMetricsConstants.MONTH_TRANSFORMER_STATSTORE, AppAOConstants.MONTH_METRICTYPE), 
				ApplicationMetricsConstants.MONTH_TRANSFORMER_STATSTORE)

		.to(kafkaStreamsConfigurationYML.getOutputTopic());


		/*****Year*****/

		stream
		.filter((k,v)-> v!=null)
		.filter((key, value) -> appService.filterEvents(eventPayloadConfigurationYML.getFilters(), value))
		.filter((k,v)-> appService.filterSubmittedDate(AppAOConstants.YEAR_METRICTYPE, v))
		.selectKey((k,v) -> k = filtertNullKey())
		.groupByKey()
		.windowedBy(TimeWindows.of(Duration.ofSeconds(kafkaStreamsConfigurationYML.getWindowSizeSeconds())))
		.aggregate(new MetricInitializer(), 
				new ApplicationMetricsAggregator(eventPayloadConfigurationYML),
				materialized(ApplicationMetricsConstants.YEAR_AGGREGATOR_STATSTORE))
		.toStream()
		.transform(() -> new MetricsTransformer(outputConfiguration,
				kafkaStreamsConfigurationYML,ApplicationMetricsConstants.YEAR_TRANSFORMER_STATSTORE, AppAOConstants.YEAR_METRICTYPE), 
				ApplicationMetricsConstants.YEAR_TRANSFORMER_STATSTORE)

		.to(kafkaStreamsConfigurationYML.getOutputTopic());

	}

	/***
	 * It is to handle the null key values
	 * @param k
	 * @return String
	 */
	private String filtertNullKey() {
		LOGGER.trace("AppKafkaStream:filtertNullKey - Handling the null key scenario");
		return "EH-Aggregation";
	}

	private boolean validateSubmittedDate(JsonNode message) {
		boolean flag=false;

		String submittedDate= JsonTool.fetchString(message, eventPayloadConfigurationYML.getAppSubmittDatePath());

		if(!submittedDate.isEmpty()) {
			try {
				ZonedDateTime.parse(submittedDate, DateTimeFormatter.ofPattern(eventPayloadConfigurationYML.getSourceTimeStampFormat()));
				flag=true;
			}catch(Exception e) {
				LOGGER.warn("LifeTime Metrics Evaluation: An issue with parsing the applicationSubmittedDate due to invalid format with the following error {}", e.getLocalizedMessage());
				return false;
			}
		}else {
			LOGGER.warn("LifeTime Metrics Evaluation: The required element {} is not available in the payload",submittedDate);
		}
		return flag;
	}

	protected WindowBytesStoreSupplier getSupplier(String stateStoreName) {

		LOGGER.info("AppKafkaStream:getSupplier - Configuring statestore {} with retention {} and window size {}",
				stateStoreName,kafkaStreamsConfigurationYML.getCleanUpPolicy(),kafkaStreamsConfigurationYML.getWindowSizeSeconds());
		return Stores.persistentWindowStore(stateStoreName, 
				Duration.ofDays(kafkaStreamsConfigurationYML.getCleanUpPolicy()), 
				Duration.ofSeconds(kafkaStreamsConfigurationYML.getWindowSizeSeconds()), Boolean.FALSE);
	}

	/***
	 * It is to configure the statestore with the required parameters.
	 * @param stateStoreName
	 * @return Materialized
	 */
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
