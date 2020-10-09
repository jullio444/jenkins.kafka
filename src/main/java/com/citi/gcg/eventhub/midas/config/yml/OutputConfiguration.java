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

@Configuration
@ConfigurationProperties(value = AppYmlConfigConstants.OUTPUT_CONFIG_YML_PREFIX)
public class OutputConfiguration {
	private static final Logger LOG = LoggerFactory.getLogger(OutputConfiguration.class);
	
	private String outputFormat;
	private JsonNode dailyOutputJsonObj;
	private ObjectMapper mapper;
	private String headerFormatTimeZone;

	public OutputConfiguration(){
		mapper = new ObjectMapper();
	}
	
	@PostConstruct
	private void init() {
		try {
			dailyOutputJsonObj = mapper.readTree(outputFormat);
			if(LOG.isInfoEnabled()) {
				LOG.info(String.format("Daily OutputOutput format: %s", dailyOutputJsonObj.toString()));
			}
		} catch (IOException exception) {
			LOG.error(String.format("Error Loading the JSON formats %s", exception.toString()));
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

}