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
	private static final String MODIFY_INPUT ="{\"event\": {\"header\": {\"businessCode\": \"GCB\",\"timeStamp\": \"2019-08-13 17:36:10 GMT\",\"producerCSI\": \"172109\",\"countrycode\": \"US\",\"name\": \"RossApplication\",\"channel\": \"MIDAS\",\"transactionTime\": \"2019-08-13 17:36:10 GMT\",\"version\": \"1.0\",\"uuid\": \"8efa5c10-dd29-11e8-8da0-00505684114a\",\"sid\": \"a996a11d-a74f-4987-8360-98008cb68330\"},\"body\": {\"operationType\": \"%s\",\"applicationId\": \"267460398254241115979192\",\"applicationVerificationCode\": \"1\",\"queueId\": \"%s\",\"previousQueueId\": \"%s\",\"channel\": \"CBOL\",\"applicationGroup\": \"%s\",\"customerType\": \"N\",\"firstInitiatorApplicationID\": \"%s\",\"createdDate\": \"2019-08-10T03:27:27.722Z\",\"fileNumber\": \"4685234\",\"trackingId\": \"6673285297\",\"promotionCode\": \"ADL123\",\"createdBy\": \"ROSS\"}}}";
	private static ObjectMapper mapper= new ObjectMapper();
	private static String setJsonValuesForInput(String operationType, String queueId, String previousQueueId, String applicationGroup, String firstInitiatorApplicationID) {
		return String.format(MODIFY_INPUT ,operationType,queueId,previousQueueId,applicationGroup,firstInitiatorApplicationID);
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
		JsonNode node1= mapper.readTree(setJsonValuesForInput("I", "ROAD", "ROFR", "GOOGLE", "901"));
		JsonNode node2= mapper.readTree(setJsonValuesForInput("insert", "ROAD", "ROFR", "GOOGLE", "901"));
		assertEquals("I", resultsExtractor.extractResultsFromData(node1, eventPayloadConfigurationYML.getConditions()).get("applicationOperation"));
		assertEquals("I", resultsExtractor.extractResultsFromData(node2, eventPayloadConfigurationYML.getConditions()).get("applicationOperation"));
	}

	@Test
	public void testExceptionTest() {
		MetricsApplicationRuntimeException exception = new MetricsApplicationRuntimeException("EXCEPTION");
		assertEquals("EXCEPTION",exception.getMessage());
	}

}

