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
package com.citi.gcg.eventhub.midas.kafka.stream.processor;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import com.citi.gcg.eventhub.midas.config.yml.KafkaStreamsConfigurationYML;
import com.citi.gcg.eventhub.midas.config.yml.OutputConfiguration;
import com.citi.gcg.eventhub.midas.constants.AppAOConstants;
import com.citi.gcg.eventhub.midas.constants.ApplicationMetricsConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/***
 * 
 * It is a kafka processor class which uses the transformed data and make it into required JSON structure, 
 *  finally it will be sending to another kafka topic
 * @author EventHub Dev Team
 *
 */

@Configuration
public class KafkaProcesor implements Processor<String, JsonNode> {

	private static final  Logger LOGGER = LoggerFactory.getLogger(KafkaProcesor.class);

	private static final String DATE_TIME_FORMAT = "MM-dd-yyyy'T'HH:mm:ss z";
	private ObjectNode output;
	private OutputConfiguration outputConfiguration;
	KafkaStreamsConfigurationYML kafkaStreamsConfigurationYML;
	KProducer kProducer;
	private ZoneId headerTimeZone;

	public KafkaProcesor(OutputConfiguration outputConfiguration,KafkaStreamsConfigurationYML kafkaStreamsConfigurationYML, KProducer kProducer) {
		this.outputConfiguration = outputConfiguration;
		this.kafkaStreamsConfigurationYML=kafkaStreamsConfigurationYML;
		this.kProducer=kProducer;
	}

	private ProcessorContext context;

	@Override
	public void init(ProcessorContext context) {
		this.context = context;

		LOGGER.info("KafkaProcesor- loading the required json as output node");

		output = (ObjectNode) outputConfiguration.getDailyOutputJsonObj();

		headerTimeZone = ZoneId.of(TimeZone.getTimeZone(outputConfiguration.getHeaderFormatTimeZone()).toZoneId().toString());
		ZonedDateTime headerDate = ZonedDateTime.now(headerTimeZone);
		output.put(AppAOConstants.TRANSACTIONDATETIME, headerDate.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
		output.put(AppAOConstants.METRICS_APP_APPROVED, ApplicationMetricsConstants.TOTAL_APPROVED);
		output.put(AppAOConstants.METRICS_APP_DECLINED, ApplicationMetricsConstants.TOTAL_DECLINED);
		output.put(AppAOConstants.METRICS_APP_PENDED, ApplicationMetricsConstants.TOTAL_PENDED);
		output.put(AppAOConstants.METRICS_APP_SUBMITTED, ApplicationMetricsConstants.TOTAL_APPLICATIONS);

	}

	@Override
	public void process(String key, JsonNode value) {
		if (key.equalsIgnoreCase(AppAOConstants.METRIC)) {
			updateDailyOutput(value);
			String message = output.toString();
			LOGGER.info("KafkaProcesor process- 1 minute metrics: {}", message);
			kProducer.sendMessage(kafkaStreamsConfigurationYML.getOutputTopic(), message);
		}
		context.commit();
	}

	private void updateDailyOutput(JsonNode value) {

		ZonedDateTime headerDate = ZonedDateTime.now(headerTimeZone);
		output.put(AppAOConstants.TRANSACTIONDATETIME, headerDate.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
		output.put(ApplicationMetricsConstants.TOTAL_APPLICATIONS, value.get(ApplicationMetricsConstants.TOTAL_APPLICATIONS).asInt());
		output.put(ApplicationMetricsConstants.TOTAL_APPROVED, value.get(ApplicationMetricsConstants.TOTAL_APPROVED).asInt());
		output.put(ApplicationMetricsConstants.TOTAL_DECLINED, value.get(ApplicationMetricsConstants.TOTAL_DECLINED).asInt());
		output.put(ApplicationMetricsConstants.TOTAL_PENDED, value.get(ApplicationMetricsConstants.TOTAL_PENDED).asInt());
	}

	@Override
	public void close() {
		context.commit();
	}

}
