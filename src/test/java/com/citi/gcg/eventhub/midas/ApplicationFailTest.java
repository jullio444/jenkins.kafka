package com.citi.gcg.eventhub.midas;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.citi.gcg.eventhub.midas.config.yml.EventPayloadConfigurationYML;
import com.citi.gcg.eventhub.midas.config.yml.OutputConfiguration;
import com.citi.gcg.eventhub.midas.exception.MetricsApplicationRuntimeException;
import com.citi.gcg.eventhub.midas.service.ResultsExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringRunner.class)
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
properties = {"server.port=1"})
@ActiveProfiles("fail")
public class ApplicationFailTest {


	@Autowired
	EventPayloadConfigurationYML eventPayloadConfigurationYML;

	@Autowired
	ResultsExtractor resultsExtractor;

	@Autowired
	OutputConfiguration outputConfiguration;

	private static final  Logger logger = LoggerFactory.getLogger(ApplicationFailTest.class);
	private static final String MODIFY_APPLICATION_INPUT="{\"event\": {\"header\": {\"name\": \"GCB.NAM.Retail.Midas.AccountOpening.Messages\",\"version\": \"1.0\",\"producerCSI\": \"169956\",\"channel\": \"MIDAS\",\"countryCode\": \"US\",\"businessCode\": \"GCB\",\"domain\": \"Acquire\",\"uuid\": \"UR-120220191142\",\"sid\": \"44d93b64-d446-475b-89cc-f54158fd516f\",\"businessTransactionTime\": \"2019-08-08 10:12:25 UTC\",\"eventTimeStamp\": \"2019-08-08 10:12:25 UTC\",\"custom\": {\"appName\": \"OAO\",\"apiKey\": \"preQualifyUserResultNotification\",\"corId\": \"d321fd3e-5f54-4fbf-8256-7d5b544e0e77\",\"tenantId\": \"%s\"}},\"body\": {\"requestId\": \"G1MQ0YERlJJLW\",\"applicationId\": \"\",\"partnerCustomerIdentifier\": \"C123456799TDPyGd\",\"citiCustomerIdentifier\": \"135429878191148\",\"epochMillis\": \"1602087421798\",\"previousStatus\": \"%s\",\"status\": \"%s\",\"context\": \"COLLECTION_CONTEXT_ACCOUNT_CREATION\",\"details\": [{\"type\": \"\",\"reason\": \"\",\"address\": {\"city\": \"\",\"state\": \"\",\"zip\": \"\"},\"dateOfBirth\": {\"month\": \"\",\"year\": \"\"}}]}}}";
	private static ObjectMapper mapper= new ObjectMapper();
	
	private static String setJsonValuesForInput(String tenatId, String previousStatus, String status) {
		return String.format(MODIFY_APPLICATION_INPUT ,tenatId,previousStatus,status);
	}

	@Test
	public void testingYMLConfigs() {

		assertNull(outputConfiguration.getDailyOutputJsonObj());	
	}

	@Test
	public void testingConditionsJsonConfig() throws JsonMappingException, JsonProcessingException {

		logger.info("Testing with different prefix of conditionsFileName");
		eventPayloadConfigurationYML.setCategorization("file2://./src/test/resources/udf_conditions.json");
		assertNull(eventPayloadConfigurationYML.getConditions());

		logger.info("Testing with the conditions JSON without having conditions section");
		eventPayloadConfigurationYML.setCategorization("file://./src/test/resources/udf_conditions2.json");
		assertNull(eventPayloadConfigurationYML.getConditions());

		logger.info("Testing with the conditions JSON without having filters section");
		eventPayloadConfigurationYML.setCategorization("file://./src/test/resources/udf_conditions4.json");
		assertNull(eventPayloadConfigurationYML.getFilters());

		logger.info("Testing with the invalid conditions JSON");
		eventPayloadConfigurationYML.setCategorization("file://./src/test/resources/udf_conditions3.json");
		assertNull(eventPayloadConfigurationYML.getConditions());
		assertNull(eventPayloadConfigurationYML.getFilters());

		logger.info("Testing with the conditions JSON which contains filterType as any in filters section");
		eventPayloadConfigurationYML.setCategorization("file://./src/test/resources/udf_conditions5.json");
		JsonNode node1= mapper.readTree(setJsonValuesForInput("MIDAS", "SOMETHING", "APPLICATION_RECEIVED"));
		JsonNode node2= mapper.readTree(setJsonValuesForInput("MIDAS",  "", "APPLICATION_RECEIVED"));
		assertEquals("I", resultsExtractor.extractResultsFromData(node1, eventPayloadConfigurationYML.getConditions()).get("applicationOperation"));
		assertEquals("I", resultsExtractor.extractResultsFromData(node2, eventPayloadConfigurationYML.getConditions()).get("applicationOperation"));
	}

	
	@Test
	public void testExceptionTest() {
		MetricsApplicationRuntimeException exception = new MetricsApplicationRuntimeException("EXCEPTION");
		assertEquals("EXCEPTION",exception.getMessage());
	}

}

