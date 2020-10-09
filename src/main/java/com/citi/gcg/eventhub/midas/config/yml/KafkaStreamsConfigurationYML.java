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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.citi.gcg.eventhub.midas.constants.AppYmlConfigConstants;

@Configuration
@ConfigurationProperties(value=AppYmlConfigConstants.EVENT_KAFKA_YML_PREFIX )
public class KafkaStreamsConfigurationYML {
	

	private int cleanUpPolicy;
	
	private long windowSizeSeconds;
	
	private String outputTopic;
	
	public String getOutputTopic() {
		return outputTopic;
	}

	public void setOutputTopic(String outputTopic) {
		this.outputTopic = outputTopic;
	}

	public KafkaStreamsConfigurationYML() {
		// TODO Auto-generated constructor stub
	}
	
	public int getCleanUpPolicy() {
		return cleanUpPolicy;
	}

	public void setCleanUpPolicy(int cleanUpPolicy) {
		this.cleanUpPolicy = cleanUpPolicy;
	}

	public long getWindowSizeSeconds() {
		return windowSizeSeconds;
	}

	public void setWindowSizeSeconds(long windowSizeSeconds) {
		this.windowSizeSeconds = windowSizeSeconds;
	}

	
}
