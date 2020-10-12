package com.citi.gcg.eventhub.midas;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.citi.gcg.eventhub.midas.config.yml.EventPayloadConfigurationYML;
import com.citi.gcg.eventhub.midas.config.yml.OutputConfiguration;
import com.citi.gcg.eventhub.midas.kafka.serde.JsonSerde;
import com.citi.gcg.eventhub.midas.service.AppService;
import com.citi.gcg.eventhub.midas.service.ResultsExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringRunner.class)
@DirtiesContext
@EmbeddedKafka
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
properties = {"server.port=1"})
@ActiveProfiles("fail")
public class ApplicationTestFail {
	
	@Autowired
	StreamsBuilderFactoryBean streamsBuilderFactoryBean;

	@Autowired
	EventPayloadConfigurationYML eventPayloadConfigurationYML;
	
	@Autowired
	OutputConfiguration outputConfiguration;
	
	@Autowired
	AppService appService;

	@Autowired
	ResultsExtractor resultsExtractor;
	
	private static final  Logger logger = LoggerFactory.getLogger(ApplicationTestFail.class);
	private static final String INPUT_TOPIC = "nam.us.retailbank.digital.raw.ross-application";
	private static final String OUTPUT_TOPIC = "nam.us.all.derived.midas.ao.metrics";
	private static final String MODIFY_INPUT ="{\"event\": {\"header\": {\"businessCode\": \"GCB\",\"timeStamp\": \"2019-08-13 17:36:10 GMT\",\"producerCSI\": \"172109\",\"countrycode\": \"US\",\"name\": \"RossApplication\",\"channel\": \"MIDAS\",\"transactionTime\": \"2019-08-13 17:36:10 GMT\",\"version\": \"1.0\",\"uuid\": \"8efa5c10-dd29-11e8-8da0-00505684114a\",\"sid\": \"a996a11d-a74f-4987-8360-98008cb68330\"},\"body\": {\"operationType\": \"%s\",\"applicationId\": \"267460398254241115979192\",\"applicationVerificationCode\": \"1\",\"queueId\": \"%s\",\"previousQueueId\": \"%s\",\"channel\": \"CBOL\",\"applicationGroup\": \"%s\",\"customerType\": \"N\",\"firstInitiatorApplicationID\": \"%s\",\"createdDate\": \"2019-08-10T03:27:27.722Z\",\"fileNumber\": \"4685234\",\"trackingId\": \"6673285297\",\"promotionCode\": \"ADL123\",\"createdBy\": \"ROSS\"}}}";
	private static final String SAMPLE_INPUT="{\"event\": {\"header\": {\"businessCode\": \"GCB\",\"timeStamp\": \"2019-08-13 17:36:10 GMT\",\"producerCSI\": \"172109\",\"countrycode\": \"US\",\"name\": \"RossApplication\",\"channel\": \"MIDAS\",\"transactionTime\": \"2019-08-13 17:36:10 GMT\",\"version\": \"1.0\",\"uuid\": \"8efa5c10-dd29-11e8-8da0-00505684114a\",\"sid\": \"a996a11d-a74f-4987-8360-98008cb68330\"},\"body\": {\"operationType\": \"I\",\"applicationId\": \"267460398254241115979192\",\"applicationVerificationCode\": \"1\",\"queueId\": \"ROAD\",\"previousQueueId\": null,\"channel\": \"CBOL\",\"applicationGroup\": \"GOOGLE\",\"customerType\": \"N\",\"firstInitiatorApplicationID\": \"901\",\"createdDate\": \"2019-08-10T03:27:27.722Z\",\"fileNumber\": \"4685234\",\"trackingId\": \"6673285297\",\"promotionCode\": \"ADL123\",\"createdBy\": \"ROSS\"}}}";
	private static final String SAMPLE_INPUT2="{\"event\": {\"header\": {\"businessCode\": \"GCB\",\"timeStamp\": \"2019-08-13 17:36:10 GMT\",\"producerCSI\": \"172109\",\"countrycode\": \"US\",\"name\": \"RossApplication\",\"channel\": \"MIDAS\",\"transactionTime\": \"2019-08-13 17:36:10 GMT\",\"version\": \"1.0\",\"uuid\": \"8efa5c10-dd29-11e8-8da0-00505684114a\",\"sid\": \"a996a11d-a74f-4987-8360-98008cb68330\"},\"body\": {\"operationType\": \"U\",\"applicationId\": \"267460398254241115979192\",\"applicationVerificationCode\": \"1\",\"queueId\": \"ROCF\",\"previousQueueId\": \"ROAD\",\"channel\": \"CBOL\",\"applicationGroup\": \"GOOGLE\",\"customerType\": \"N\",\"firstInitiatorApplicationID\": \"901\",\"createdDate\": \"2019-08-10T03:27:27.722Z\",\"fileNumber\": \"4685234\",\"trackingId\": \"6673285297\",\"promotionCode\": \"ADL123\",\"createdBy\": \"ROSS\"}}}";
	
	private static final String APP_ID = "I_STR_MIDAS_A_MDB_01_EHTEST2";
	private static final String GRP_ID = "I_STR_MIDAS_A_MDB_01_EHTEST2";
	private static final String RESET_OFFSET = "latest";
	private static final String PROPERTY_BROKERS = "spring.cloud.stream.kafka.streams.binder.brokers";
	private static final String PROPERTY_APP_ID= "spring.cloud.stream.kafka.streams.binder.configuration.application.id";
	private static final String PROPERTY_GROUP_ID = "spring.cloud.stream.kafka.streams.binder.configuration.group.id";
	private static final String PROPERTY_OFFSET_RESET = "spring.cloud.stream.kafka.streams.binder.configuration.consumer.auto.offset.reset";
	private static Producer<String, JsonNode> producer;
	private static Consumer<String, String> consumer;
	private static ObjectMapper mapper= new ObjectMapper();
	
	@ClassRule
	public static final EmbeddedKafkaRule embedded = new EmbeddedKafkaRule(1, Boolean.TRUE,1,INPUT_TOPIC,OUTPUT_TOPIC);



	@BeforeClass
	public static void setup() throws JsonMappingException, JsonProcessingException {
		//Broker setup
		embedded.kafkaPorts(51817);
		embedded.getEmbeddedKafka().kafkaPorts(51817);
		embedded.getEmbeddedKafka().getTopics().clear();
		embedded.getEmbeddedKafka().afterPropertiesSet();
		System.setProperty(PROPERTY_BROKERS, embedded.getEmbeddedKafka().getBrokersAsString());
		System.setProperty(PROPERTY_APP_ID, APP_ID);
		System.setProperty(PROPERTY_GROUP_ID, GRP_ID);
		System.setProperty(PROPERTY_OFFSET_RESET, RESET_OFFSET);
		//Producer creation
		setProducer();

	}

	@AfterClass
	public static void tearDown() {
		//consumer.close();
		System.clearProperty("spring.cloud.stream.kafka.streams.binder.brokers");
	}

	@SuppressWarnings("resource")
	private static void setProducer() {
		Map<String, Object> producerProperties = KafkaTestUtils.producerProps(embedded.getEmbeddedKafka());
		producer = new DefaultKafkaProducerFactory<>(producerProperties, new StringSerializer(),  new JsonSerde().serializer()).createProducer();
	}
	private static String setJsonValuesForInput(String operationType, String queueId, String previousQueueId, String applicationGroup, String firstInitiatorApplicationID) {
		return String.format(MODIFY_INPUT ,operationType,queueId,previousQueueId,applicationGroup,firstInitiatorApplicationID);
	}
	

	
	@Test
	public void testingYMLConfigs() {
		assertEquals("file2://./src/test/resources/udf_conditions.json", eventPayloadConfigurationYML.getCategorization());
		assertNotNull(outputConfiguration.getOutputFormat());	
	}
	
	
}
	
