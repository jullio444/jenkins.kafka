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
public class ApplicationTest {

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

	private static final  Logger logger = LoggerFactory.getLogger(ApplicationTest.class);
	private static final String INPUT_TOPIC = "nam.us.retailbank.digital.raw.ross-application";
	private static final String OUTPUT_TOPIC = "nam.us.all.derived.midas.ao.metrics";

	private static final String SAMPLE_INPUT="{\"event\": {\"header\": {\"name\": \"GCB.NAM.Retail.Midas.AccountOpening.Messages\",\"version\": \"1.0\",\"producerCSI\": \"169956\",\"channel\": \"MIDAS\",\"countryCode\": \"US\",\"businessCode\": \"GCB\",\"domain\": \"Acquire\",\"uuid\": \"UR-120220191142\",\"sid\": \"44d93b64-d446-475b-89cc-f54158fd516f\",\"businessTransactionTime\": \"2019-08-08 10:12:25 UTC\",\"eventTimeStamp\": \"2019-08-08 10:12:25 UTC\",\"custom\": {\"appName\": \"OAO\",\"apiKey\": \"preQualifyUserResultNotification\",\"corId\": \"d321fd3e-5f54-4fbf-8256-7d5b544e0e77\",\"tenantId\": \"MIDAS\",\"partnerId\": \"GOOGLE\",\"status\": \"APPLICATION_RECEIVED\"}},\"body\": {\"requestId\": \"G1MQ0YERlJJLW\",\"applicationId\": \"\",\"partnerCustomerIdentifier\": \"C123456799TDPyGd\",\"citiCustomerIdentifier\": \"135429878191148\",\"epochMillis\": \"1602087421798\",\"previous_status\": \"\",\"status\": \"APPLICATION_RECEIVED\",\"context\": \"COLLECTION_CONTEXT_ACCOUNT_CREATION\",\"details\": [{\"type\": \"\",\"reason\": \"\",\"address\": {\"city\": \"\",\"state\": \"\",\"zip\": \"\"},\"dateOfBirth\": {\"month\": \"\",\"year\": \"\"}}]}}}";
	
	
	private static final String MODIFY_APPLICATION_INPUT="{\"event\": {\"header\": {\"name\": \"GCB.NAM.Retail.Midas.AccountOpening.Messages\",\"version\": \"1.0\",\"producerCSI\": \"169956\",\"channel\": \"MIDAS\",\"countryCode\": \"US\",\"businessCode\": \"GCB\",\"domain\": \"Acquire\",\"uuid\": \"UR-120220191142\",\"sid\": \"44d93b64-d446-475b-89cc-f54158fd516f\",\"businessTransactionTime\": \"2019-08-08 10:12:25 UTC\",\"eventTimeStamp\": \"2019-08-08 10:12:25 UTC\",\"custom\": {\"appName\": \"OAO\",\"apiKey\": \"preQualifyUserResultNotification\",\"corId\": \"d321fd3e-5f54-4fbf-8256-7d5b544e0e77\",\"tenantId\": \"%s\",\"partnerId\": \"%s\",\"status\": \"APPLICATION_RECEIVED\"}},\"body\": {\"requestId\": \"G1MQ0YERlJJLW\",\"applicationId\": \"\",\"partnerCustomerIdentifier\": \"C123456799TDPyGd\",\"citiCustomerIdentifier\": \"135429878191148\",\"epochMillis\": \"1602087421798\",\"previous_status\": \"%s\",\"status\": \"%s\",\"context\": \"COLLECTION_CONTEXT_ACCOUNT_CREATION\",\"details\": [{\"type\": \"\",\"reason\": \"\",\"address\": {\"city\": \"\",\"state\": \"\",\"zip\": \"\"},\"dateOfBirth\": {\"month\": \"\",\"year\": \"\"}}]}}}";
	
	private static final String ACCOUNT_OPEN_SAMPLE1="{\"event\": {\"header\": {\"name\": \"GCB/NAM/US/Retail/Midas/AccountManagement/AccountOpening/AccountOpened\",\"version\": \"1.0\",\"producerCSI\": \"169956\",\"channel\": \"MIDAS\",\"countryCode\": \"US\",\"businessCode\": \"GCB\",\"domain\": \"Acquire\",\"uuid\": \"UR-120220191142\",\"sid\": \"44d93b64-d446-475b-89cc-f54158fd516f\",\"businessTransactionTime\": \"2019-08-08 10:12:25 UTC\",\"eventTimeStamp\": \"2019-08-08 10:12:25 UTC\",\"custom\": {\"appName\": \"OAO\",\"apiKey\": \"openAccount\",\"corId\": \"e621fd3e-5f54-4fbf-8256-7d5b544e0e77\",\"tenantId\": \"MIDAS\",\"partnerId\": \"GOOGLE\",\"status\": \"OPENED\"}},\"body\": {\"requestId\": \"263bb041-7d29-4a27-8908-dce3e2a0e397\",\"applicationId\": \"1111\",\"accountNumber\": \"40500101823\",\"previous_status\": \"APPROVED\",\"status\": \"OPENED\",\"customerType\": \"PRIMARY_CUSTOMER\",\"cin\": \"5156770400055579\",\"type\": \"071\",\"class\": \"009\",\"partnerID\": \"192b9d08d3e4fd36ee83721674b02f3c\",\"citiCustomerRefID\": \"135429878191148\",\"valuePropCode\": \"051\",\"gcfId\": \"20280137144250600\"}}}";
	private static final String ACCOUNT_OPEN_SAMPLE2="{\"event\": {\"header\": {\"name\": \"GCB/NAM/US/Retail/Midas/AccountManagement/AccountOpening/AccountOpened\",\"version\": \"1.0\",\"producerCSI\": \"169956\",\"channel\": \"MIDAS\",\"countryCode\": \"US\",\"businessCode\": \"GCB\",\"domain\": \"Acquire\",\"uuid\": \"UR-120220191142\",\"sid\": \"44d93b64-d446-475b-89cc-f54158fd516f\",\"businessTransactionTime\": \"2019-08-08 10:12:25 UTC\",\"eventTimeStamp\": \"2019-08-08 10:12:25 UTC\",\"custom\": {\"appName\": \"OAO\",\"apiKey\": \"openAccount\",\"corId\": \"e621fd3e-5f54-4fbf-8256-7d5b544e0e77\",\"tenantId\": \"MIDAS\",\"partnerId\": \"GOOGLE\",\"status\": \"OPENED\"}},\"body\": {\"requestId\": \"263bb041-7d29-4a27-8908-dce3e2a0e397\",\"applicationId\": \"1111\",\"accountNumber\": \"40500101823\",\"previous_status\": \"APPROVED\",\"status\": \"OPENED\",\"customerType\": \"PRIMARY_CUSTOMER\",\"cin\": \"5156770400055579\",\"type\": \"038\",\"class\": \"001\",\"partnerID\": \"192b9d08d3e4fd36ee83721674b02f3c\",\"citiCustomerRefID\": \"135429878191148\",\"valuePropCode\": \"051\",\"gcfId\": \"20280137144250600\"}}}";
	private static final String ACCOUNT_OPEN_SAMPLE3="{\"event\": {\"header\": {\"name\": \"GCB/NAM/US/Retail/Midas/AccountManagement/AccountOpening/AccountOpened\",\"version\": \"1.0\",\"producerCSI\": \"169956\",\"channel\": \"MIDAS\",\"countryCode\": \"US\",\"businessCode\": \"GCB\",\"domain\": \"Acquire\",\"uuid\": \"UR-120220191142\",\"sid\": \"44d93b64-d446-475b-89cc-f54158fd516f\",\"businessTransactionTime\": \"2019-08-08 10:12:25 UTC\",\"eventTimeStamp\": \"2019-08-08 10:12:25 UTC\",\"custom\": {\"appName\": \"OAO\",\"apiKey\": \"openAccount\",\"corId\": \"e621fd3e-5f54-4fbf-8256-7d5b544e0e77\",\"tenantId\": \"MIDAS\",\"partnerId\": \"GOOGLE\",\"status\": \"OPENED\"}},\"body\": {\"requestId\": \"263bb041-7d29-4a27-8908-dce3e2a0e397\",\"applicationId\": \"1111\",\"accountNumber\": \"40500101823\",\"previous_status\": \"APPROVED\",\"status\": \"OPENED\",\"customerType\": \"PRIMARY_CUSTOMER\",\"cin\": \"5156770400055579\",\"type\": \"004\",\"class\": \"038\",\"partnerID\": \"192b9d08d3e4fd36ee83721674b02f3c\",\"citiCustomerRefID\": \"135429878191148\",\"valuePropCode\": \"051\",\"gcfId\": \"20280137144250600\"}}}";
	


	private static final String APP_ID = "I_STR_MIDAS_A_MDB_01_EHTEST";
	private static final String GRP_ID = "I_STR_MIDAS_A_MDB_01_EHTEST";
	private static final String RESET_OFFSET = "latest";
	private static final String PROPERTY_BROKERS = "spring.cloud.stream.kafka.streams.binder.brokers";
	private static final String PROPERTY_APP_ID= "spring.cloud.stream.kafka.streams.binder.configuration.application.id";
	private static final String PROPERTY_GROUP_ID = "spring.cloud.stream.kafka.streams.binder.configuration.group.id";
	private static final String PROPERTY_OFFSET_RESET = "spring.cloud.stream.kafka.streams.binder.configuration.consumer.auto.offset.reset";
	private static final String FALSE = "false";
	private static final int POLL_TIME = 20;

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
		consumer.close();
		System.clearProperty("spring.cloud.stream.kafka.streams.binder.brokers");
	}

	@SuppressWarnings("resource")
	private static void setProducer() {
		Map<String, Object> producerProperties = KafkaTestUtils.producerProps(embedded.getEmbeddedKafka());
		producer = new DefaultKafkaProducerFactory<>(producerProperties, new StringSerializer(),  new JsonSerde().serializer()).createProducer();
	}
	private static void setConsumer(String topic) {
		Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), FALSE, embedded.getEmbeddedKafka());
		consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, RESET_OFFSET);
		consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
		consumer = cf.createConsumer();
		consumer.subscribe(Collections.singletonList(topic));
		consumer.poll(Duration.ofSeconds(POLL_TIME));
	}

	private static String setJsonValuesForInput(String tenatId, String partnerId, String previousStatus, String status) {
		return String.format(MODIFY_APPLICATION_INPUT ,tenatId,partnerId,previousStatus,status);
	}

	@Test
	public void testingJson1() throws InterruptedException, ExecutionException, JsonMappingException, JsonProcessingException {
		JsonNode node1= mapper.readTree(SAMPLE_INPUT);
		assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC, node1)).get().hasOffset());
		TimeUnit.MILLISECONDS.sleep(30000);
	}

	@Test
	public void testingJsonWithKey() throws InterruptedException, ExecutionException, JsonMappingException, JsonProcessingException {
		JsonNode node1= mapper.readTree(SAMPLE_INPUT);
		assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,"Midas_AO", node1)).get().hasOffset());
		TimeUnit.MILLISECONDS.sleep(30000);
	}

	@Test
	public void testingJsonWithUpdateOperation() throws InterruptedException, ExecutionException, JsonMappingException, JsonProcessingException {
		setConsumer(OUTPUT_TOPIC);
		JsonNode node1= mapper.readTree(setJsonValuesForInput("MIDAS", "GOOGLE", "APPLICATION_RECEIVED", "APPLICATION_APPROVED"));
		assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,null, node1)).get().hasOffset());
		ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(POLL_TIME));
		TimeUnit.MILLISECONDS.sleep(30000);
		records.forEach(record -> {
			assertNotNull(record.value());
		});
		consumer.close();
	}

	@Test
	public void testingJsonWithInsertOperation() throws InterruptedException, ExecutionException, JsonMappingException, JsonProcessingException {

		setConsumer(OUTPUT_TOPIC);
		logger.info("Testing with applicationSubmitted, pended apps and 2 records sent");	
		JsonNode node1= mapper.readTree(setJsonValuesForInput("MIDAS", "GOOGLE", null, "APPLICATION_RECEIVED"));
		assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,"SampleKey", node1)).get().hasOffset());
		assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,"SampleKey", node1)).get().hasOffset());
		ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(POLL_TIME));
		TimeUnit.MILLISECONDS.sleep(30000);
		records.forEach(record -> {
			assertNotNull(record.value());
			try {
				JsonNode outputNode= mapper.readTree(record.value());
				assertEquals(2, outputNode.get("applicationsSubmitted"));
			} catch (JsonMappingException e) {

				e.printStackTrace();
			} catch (JsonProcessingException e) {

				e.printStackTrace();
			}



		});

		consumer.close();
	}

	@Test
	public void testingJsonWithBothUperations() throws JsonMappingException, JsonProcessingException, InterruptedException, ExecutionException {

		setConsumer(OUTPUT_TOPIC);
		JsonNode node1= mapper.readTree(setJsonValuesForInput("MIDAS", "GOOGLE", "", "APPLICATION_RECEIVED"));
		JsonNode node2= mapper.readTree(setJsonValuesForInput("MIDAS", "GOOGLE", "APPLICATION_RECEIVED", "APPLICATION_APPROVED"));
		JsonNode node3= mapper.readTree(setJsonValuesForInput("MIDAS", "GOOGLE", "APPLICATION_RECEIVED", "APPLICATION_DECLINED"));

		logger.info("Testing with application submitted, pended apps and 5 records sent");

		for(int i=0;i<5;i++) {
			assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,null, node1)).get().hasOffset());
		}

		logger.info("Testing with Update operation from pended to approved apps and 2 records sent");

		for(int i=0;i<2;i++) {
			assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,null, node2)).get().hasOffset());
		}
		
		logger.info("Testing with Update operation from pended to declined apps and 1 record sent");

		for(int i=0;i<1;i++) {
			assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,null, node3)).get().hasOffset());
		}

		ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(POLL_TIME));
		TimeUnit.MILLISECONDS.sleep(30000);
		records.forEach(record -> {
			assertNotNull(record.value());
			try {
				JsonNode outputNode= mapper.readTree(record.value());
				assertEquals(5, outputNode.get("applicationsSubmitted"));
				assertEquals(2, outputNode.get("applicationsApproved"));
				assertEquals(2, outputNode.get("applicationsPended"));
				assertEquals(2, outputNode.get("applicationsPendedToApproved"));
				assertEquals(1, outputNode.get("applicationsPendedToDeclined"));
			} catch (JsonMappingException e) {

				e.printStackTrace();
			} catch (JsonProcessingException e) {

				e.printStackTrace();
			}
		});
		consumer.close();
	}

	@Test
	public void testingAccountMetrics() throws InterruptedException, ExecutionException, JsonMappingException, JsonProcessingException {

		setConsumer(OUTPUT_TOPIC);
		logger.info("Testing with accountopen json, with savings and checking types");	
		JsonNode node1= mapper.readTree(ACCOUNT_OPEN_SAMPLE1);
		JsonNode node2= mapper.readTree(ACCOUNT_OPEN_SAMPLE2);
		assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,"SampleKey", node1)).get().hasOffset());
		assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,"SampleKey", node2)).get().hasOffset());
		ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(POLL_TIME));
		TimeUnit.MILLISECONDS.sleep(30000);
		records.forEach(record -> {
			assertNotNull(record.value());
			try {
				JsonNode outputNode= mapper.readTree(record.value());
				assertEquals(2, outputNode.get("totalAccountsOpened"));
				assertEquals(1, outputNode.get("totalSavingsAccountsOpened"));
				assertEquals(1, outputNode.get("totalCheckingAccountsOpened"));
			} catch (JsonMappingException e) {

				e.printStackTrace();
			} catch (JsonProcessingException e) {

				e.printStackTrace();
			}



		});

		consumer.close();
	}
	@Test
	public void testingYMLConfigs() {
		assertEquals("file://./src/test/resources/udf_conditions.json", eventPayloadConfigurationYML.getCategorization());
		assertNotNull(outputConfiguration.getOutputFormat());	
	}

	@Test
	public void testFilterService() throws JsonMappingException, JsonProcessingException {
		logger.info("Testing other filter conditions");
		JsonNode node1= mapper.readTree(setJsonValuesForInput("MIDAS", "GOOGLE", "APPLICATION_RECEIVED", "APPLICATION_APPROVED"));
		JsonNode node2= mapper.readTree(setJsonValuesForInput("MIDAS", "ANOTHER_PRODUCT", "APPLICATION_RECEIVED", "APPLICATION_APPROVED"));
		assertTrue(appService.filterEvents(eventPayloadConfigurationYML.getFilters(), node1));
		assertTrue(appService.filterEvents(null, node1));
		assertFalse(appService.filterEvents(eventPayloadConfigurationYML.getFilters(), node2));	
	}

	@Test
	public void testApplicationStatusEvaluation() throws JsonMappingException, JsonProcessingException {
		JsonNode node1= mapper.readTree(setJsonValuesForInput("MIDAS", "GOOGLE", "APPLICATION_RECEIVED", "APPLICATION_APPROVED"));
		JsonNode node2= mapper.readTree(setJsonValuesForInput("MIDAS", "GOOGLE", "APPLICATION_RECEIVED", "APPLICATION_APPROVED"));
		JsonNode node3= mapper.readTree(setJsonValuesForInput("MIDAS", "GOOGLE", "APPLICATION_RECEIVED", "APPLICATION_DECLINED"));
		JsonNode node4= mapper.readTree(setJsonValuesForInput("MIDAS", "GOOGLE", "APPLICATION_RECEIVED", "INVALID_STATUS"));
		JsonNode node5= mapper.readTree(setJsonValuesForInput("MIDAS", "GOOGLE", "INVALID_PREVIOUS_STATUS", "APPLICATION_APPROVED"));
		JsonNode node6= mapper.readTree(setJsonValuesForInput("MIDAS", "GOOGLE", "INVALID_PREVIOUS_STATUS", "INVALID_STATUS"));
		JsonNode node7= mapper.readTree(setJsonValuesForInput("MIDAS", "GOOGLE", "APPLICATION_RECEIVED", "APPLICATION_PENDED_ON_ADDITIONAL_DOCS"));
		assertEquals("applications_approved", resultsExtractor.extractResultsFromData(node1, eventPayloadConfigurationYML.getConditions()).get("applicationStatus"));
		assertEquals("applications_pended", resultsExtractor.extractResultsFromData(node2, eventPayloadConfigurationYML.getConditions()).get("previousApplicationStatus"));
		assertEquals("applications_approved", resultsExtractor.extractResultsFromData(node2, eventPayloadConfigurationYML.getConditions()).get("applicationStatus"));
		assertEquals("applications_declined", resultsExtractor.extractResultsFromData(node3, eventPayloadConfigurationYML.getConditions()).get("applicationStatus"));

		logger.info("Testing other applicationStatus conditions");
		assertEquals("null", resultsExtractor.extractResultsFromData(node4, eventPayloadConfigurationYML.getConditions()).get("applicationStatus"));
		assertEquals("null", resultsExtractor.extractResultsFromData(node5, eventPayloadConfigurationYML.getConditions()).get("previousApplicationStatus"));
		assertEquals("null", resultsExtractor.extractResultsFromData(node6, eventPayloadConfigurationYML.getConditions()).get("applicationStatus"));
		assertEquals("null", resultsExtractor.extractResultsFromData(node6, eventPayloadConfigurationYML.getConditions()).get("previousApplicationStatus"));
		assertEquals("applications_pended", resultsExtractor.extractResultsFromData(node7, eventPayloadConfigurationYML.getConditions()).get("applicationStatus"));
		assertEquals("applications_pended", resultsExtractor.extractResultsFromData(node7, eventPayloadConfigurationYML.getConditions()).get("previousApplicationStatus"));

	}
	
	@Test
	public void testAccountMetricEvaluation() throws JsonMappingException, JsonProcessingException {
		JsonNode node1= mapper.readTree(ACCOUNT_OPEN_SAMPLE1);
		JsonNode node2= mapper.readTree(ACCOUNT_OPEN_SAMPLE2);
		JsonNode node3= mapper.readTree(ACCOUNT_OPEN_SAMPLE3);
		assertEquals("accountOpen", resultsExtractor.extractResultsFromData(node1, eventPayloadConfigurationYML.getConditions()).get("accountStatus"));
		assertEquals("accounts_savings", resultsExtractor.extractResultsFromData(node1, eventPayloadConfigurationYML.getConditions()).get("accountType"));
		assertEquals("accounts_checkings", resultsExtractor.extractResultsFromData(node2, eventPayloadConfigurationYML.getConditions()).get("accountType"));
		assertEquals("accountOpen", resultsExtractor.extractResultsFromData(node3, eventPayloadConfigurationYML.getConditions()).get("accountStatus"));
		assertEquals("null", resultsExtractor.extractResultsFromData(node3, eventPayloadConfigurationYML.getConditions()).get("accountType"));

	}
}

