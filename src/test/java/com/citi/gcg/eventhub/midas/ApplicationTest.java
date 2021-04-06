package com.citi.gcg.eventhub.midas;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.citi.gcg.eventhub.midas.config.yml.EventPayloadConfigurationYML;
import com.citi.gcg.eventhub.midas.config.yml.OutputConfiguration;
import com.citi.gcg.eventhub.midas.constants.ApplicationMetricsConstants;
import com.citi.gcg.eventhub.midas.kafka.serde.JsonSerde;
import com.citi.gcg.eventhub.midas.service.AppService;
import com.citi.gcg.eventhub.midas.service.JsonTool;
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
	private EventPayloadConfigurationYML eventPayloadConfigurationYML;

	@Autowired
	private OutputConfiguration outputConfiguration;

	@Autowired
	private AppService appService;

	@Autowired
	private ResultsExtractor resultsExtractor;

	private static final  Logger logger = LoggerFactory.getLogger(ApplicationTest.class);
	private static final String INPUT_TOPIC = "nam.us.retailbank.digital.raw.ross-application";
	private static final String OUTPUT_TOPIC = "nam.us.all.derived.midas.ao.metrics";

	private static final String SAMPLE_INPUT="{\"event\": {\"header\": {\"name\": \"GCB.NAM.Retail.Midas.AccountOpening.Messages\",\"version\": \"1.0\",\"producerCSI\": \"169956\",\"channel\": \"MIDAS\",\"countryCode\": \"US\",\"businessCode\": \"GCB\",\"domain\": \"Acquire\",\"uuid\": \"UR-120220191142\",\"sid\": \"44d93b64-d446-475b-89cc-f54158fd516f\",\"businessTransactionTime\": \"2019-08-08 10:12:25 UTC\",\"eventTimeStamp\": \"2019-08-08 10:12:25 UTC\",\"custom\": {\"appName\": \"OAO\",\"apiKey\": \"preQualifyUserResultNotification\",\"corId\": \"d321fd3e-5f54-4fbf-8256-7d5b544e0e77\",\"tenantId\": \"MIDAS\"}},\"body\": {\"requestId\": \"G1MQ0YERlJJLW\",\"applicationId\": \"\",\"partnerCustomerIdentifier\": \"C123456799TDPyGd\",\"citiCustomerIdentifier\": \"135429878191148\",\"epochMillis\": \"1602087421798\",\"previousStatus\": \"\",\"status\": \"APPLICATION_RECEIVED\",\"context\": \"COLLECTION_CONTEXT_ACCOUNT_CREATION\",\"details\": [{\"type\": \"\",\"reason\": \"\",\"address\": {\"city\": \"\",\"state\": \"TX\",\"zip\": \"\"},\"dateOfBirth\": {\"month\": \"\",\"year\": \"\"}}]}}}";


	private static final String MODIFY_APPLICATION_INPUT="{\"event\": {\"header\": {\"name\": \"GCB.NAM.Retail.Midas.AccountOpening.Messages\",\"version\": \"1.0\",\"producerCSI\": \"169956\",\"channel\": \"MIDAS\",\"countryCode\": \"US\",\"businessCode\": \"GCB\",\"domain\": \"Acquire\",\"uuid\": \"UR-120220191142\",\"sid\": \"44d93b64-d446-475b-89cc-f54158fd516f\",\"businessTransactionTime\": \"2019-08-08 10:12:25 UTC\",\"eventTimeStamp\": \"2019-08-08 10:12:25 UTC\",\"custom\": {\"appName\": \"OAO\",\"apiKey\": \"preQualifyUserResultNotification\",\"corId\": \"d321fd3e-5f54-4fbf-8256-7d5b544e0e77\",\"tenantId\": \"%s\"}},\"body\": {\"requestId\": \"G1MQ0YERlJJLW\",\"applicationId\": \"\",\"partnerCustomerIdentifier\": \"C123456799TDPyGd\",\"citiCustomerIdentifier\": \"135429878191148\",\"epochMillis\": \"1602087421798\",\"applicationSubmittedDate\":\"%s\",\"previousStatus\": \"%s\",\"status\": \"%s\",\"context\": \"COLLECTION_CONTEXT_ACCOUNT_CREATION\",\"details\": [{\"type\": \"\",\"reason\": \"\",\"address\": {\"city\": \"\",\"state\": \"\",\"zip\": \"\"},\"dateOfBirth\": {\"month\": \"\",\"year\": \"\"}}]}}}";


	private static final String ACCOUNT_OPEN_SAMPLE1="{\"event\": {\"header\": {\"name\": \"GCB/NAM/US/Retail/Midas/AccountManagement/AccountOpening/AccountOpened\",\"version\": \"1.0\",\"producerCSI\": \"169956\",\"channel\": \"MIDAS\",\"countryCode\": \"US\",\"businessCode\": \"GCB\",\"domain\": \"Acquire\",\"uuid\": \"UR-120220191142\",\"sid\": \"44d93b64-d446-475b-89cc-f54158fd516f\",\"businessTransactionTime\": \"2019-08-08 10:12:25 UTC\",\"eventTimeStamp\": \"2019-08-08 10:12:25 UTC\",\"custom\": {\"appName\": \"OAO\",\"apiKey\": \"openAccount\",\"corId\": \"e621fd3e-5f54-4fbf-8256-7d5b544e0e77\",\"tenantId\": \"MIDAS\"}},\"body\": {\"requestId\": \"263bb041-7d29-4a27-8908-dce3e2a0e397\",\"applicationId\": \"1111\",\"accountNumber\": \"40500101823\",\"applicationSubmittedDate\":\"2021-02-24 23:45:58 UTC\",\"previousStatus\": \"\",\"status\": \"OPENED\",\"customerType\": \"PRIMARY_CUSTOMER\",\"cin\": \"5156770400055579\",\"type\": \"071\",\"class\": \"009\",\"partnerID\": \"192b9d08d3e4fd36ee83721674b02f3c\",\"citiCustomerRefID\": \"135429878191148\",\"valuePropCode\": \"051\",\"gcfId\": \"20280137144250600\"}}}";
	private static final String ACCOUNT_OPEN_SAMPLE2="{\"event\": {\"header\": {\"name\": \"GCB/NAM/US/Retail/Midas/AccountManagement/AccountOpening/AccountOpened\",\"version\": \"1.0\",\"producerCSI\": \"169956\",\"channel\": \"MIDAS\",\"countryCode\": \"US\",\"businessCode\": \"GCB\",\"domain\": \"Acquire\",\"uuid\": \"UR-120220191142\",\"sid\": \"44d93b64-d446-475b-89cc-f54158fd516f\",\"businessTransactionTime\": \"2019-08-08 10:12:25 UTC\",\"eventTimeStamp\": \"2019-08-08 10:12:25 UTC\",\"custom\": {\"appName\": \"OAO\",\"apiKey\": \"openAccount\",\"corId\": \"e621fd3e-5f54-4fbf-8256-7d5b544e0e77\",\"tenantId\": \"MIDAS\"}},\"body\": {\"requestId\": \"263bb041-7d29-4a27-8908-dce3e2a0e397\",\"applicationId\": \"1111\",\"accountNumber\": \"40500101823\",\"applicationSubmittedDate\":\"2021-02-24 23:45:58 UTC\",\"previousStatus\": \"\",\"status\": \"OPENED\",\"customerType\": \"PRIMARY_CUSTOMER\",\"cin\": \"5156770400055579\",\"type\": \"038\",\"class\": \"001\",\"partnerID\": \"192b9d08d3e4fd36ee83721674b02f3c\",\"citiCustomerRefID\": \"135429878191148\",\"valuePropCode\": \"051\",\"gcfId\": \"20280137144250600\"}}}";
	private static final String ACCOUNT_OPEN_SAMPLE3="{\"event\": {\"header\": {\"name\": \"GCB/NAM/US/Retail/Midas/AccountManagement/AccountOpening/AccountOpened\",\"version\": \"1.0\",\"producerCSI\": \"169956\",\"channel\": \"MIDAS\",\"countryCode\": \"US\",\"businessCode\": \"GCB\",\"domain\": \"Acquire\",\"uuid\": \"UR-120220191142\",\"sid\": \"44d93b64-d446-475b-89cc-f54158fd516f\",\"businessTransactionTime\": \"2019-08-08 10:12:25 UTC\",\"eventTimeStamp\": \"2019-08-08 10:12:25 UTC\",\"custom\": {\"appName\": \"OAO\",\"apiKey\": \"openAccount\",\"corId\": \"e621fd3e-5f54-4fbf-8256-7d5b544e0e77\",\"tenantId\": \"MIDAS\"}},\"applicationSubmittedDate\":\"2021-02-24 23:45:58 UT\",\"body\": {\"requestId\": \"263bb041-7d29-4a27-8908-dce3e2a0e397\",\"applicationId\": \"1111\",\"accountNumber\": \"40500101823\",\"previousStatus\": \"\",\"status\": \"OPENED\",\"customerType\": \"PRIMARY_CUSTOMER\",\"cin\": \"5156770400055579\",\"type\": \"004\",\"class\": \"038\",\"partnerID\": \"192b9d08d3e4fd36ee83721674b02f3c\",\"citiCustomerRefID\": \"135429878191148\",\"valuePropCode\": \"051\",\"gcfId\": \"20280137144250600\"}}}";



	private static final String APP_ID = "I_STR_MIDAS_A_MDB_02_EHTEST";
	private static final String GRP_ID = "I_STR_MIDAS_A_MDB_02_EHTEST";
	private static final String RESET_OFFSET = "latest";
	private static final String PROPERTY_BROKERS = "spring.cloud.stream.kafka.streams.binder.brokers";
	private static final String PROPERTY_APP_ID= "spring.cloud.stream.kafka.streams.binder.configuration.application.id";
	private static final String PROPERTY_GROUP_ID = "spring.cloud.stream.kafka.streams.binder.configuration.group.id";
	private static final String PROPERTY_OFFSET_RESET = "spring.cloud.stream.kafka.streams.binder.configuration.consumer.auto.offset.reset";
	private static final String FALSE = "false";
	private static final int POLL_TIME = 2;

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
	private static Consumer<String, String> setConsumer(String topic) {
		Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), FALSE, embedded.getEmbeddedKafka());
		consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, RESET_OFFSET);
		consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
		consumer = cf.createConsumer();
		consumer.subscribe(Collections.singletonList(topic));
		consumer.poll(Duration.ofSeconds(POLL_TIME));
		return consumer;
	}

	private static String setJsonValuesForInput(String tenatId ,String applicationSubmittedDate, String previousStatus, String status) {
		return String.format(MODIFY_APPLICATION_INPUT ,tenatId,applicationSubmittedDate,previousStatus,status);
	}
	
	private static List<JsonNode> fetchRecordsFromConsumer(ConsumerRecords<String, String> records) throws JsonMappingException, JsonProcessingException{
		List<JsonNode> outputNodes= new ArrayList<>();;
		Iterator<ConsumerRecord<String, String>> recordIterator = records.iterator();
		while(recordIterator.hasNext()) {
			JsonNode temNode=mapper.readTree(recordIterator.next().value());
			outputNodes.add(temNode);
		}
		return outputNodes;
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
		Consumer<String,String> consumer=setConsumer(OUTPUT_TOPIC);
		SimpleDateFormat dateFormatter = new SimpleDateFormat(eventPayloadConfigurationYML.getSourceTimeStampFormat());
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = new Date();
		JsonNode node1= mapper.readTree(setJsonValuesForInput("MIDAS",dateFormatter.format(date), "APPLICATION_RECEIVED", "APPLICATION_APPROVED"));
		assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,null, node1)).get().hasOffset());
		TimeUnit.MILLISECONDS.sleep(30000);
		ConsumerRecords<String, String> records=KafkaTestUtils.getRecords(consumer);
		List<JsonNode> receivedOutputs= fetchRecordsFromConsumer(records);
		for(JsonNode output: receivedOutputs) {
			if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("MINUTE")) {
				assertEquals(1, output.get("applicationsApproved").asInt());
			}else if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("DAY")) {
				assertEquals(1, output.get("applicationsApproved").asInt());
			}else if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("MONTH")) {
				assertEquals(1, output.get("applicationsApproved").asInt());
			}else if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("YEAR")) {
				assertEquals(1, output.get("applicationsApproved").asInt());
			}	
		}

		consumer.close();
	}

	@Test
	public void testingJsonWithInsertOperation() throws InterruptedException, ExecutionException, JsonMappingException, JsonProcessingException {

		Consumer<String,String> consumer=setConsumer(OUTPUT_TOPIC);
		SimpleDateFormat dateFormatter = new SimpleDateFormat(eventPayloadConfigurationYML.getSourceTimeStampFormat());
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = new Date();
		logger.info("Testing with applicationSubmitted, pended apps and 2 records sent");	
		JsonNode node1= mapper.readTree(setJsonValuesForInput("MIDAS",dateFormatter.format(date),"", "APPLICATION_RECEIVED"));
		assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,"SampleKey", node1)).get().hasOffset());
		assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,"SampleKey", node1)).get().hasOffset());
		TimeUnit.MILLISECONDS.sleep(30000);	
		ConsumerRecords<String, String> records=KafkaTestUtils.getRecords(consumer);
		List<JsonNode> receivedOutputs= fetchRecordsFromConsumer(records);
		for(JsonNode output: receivedOutputs) {
			if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("MINUTE")) {
				assertEquals(2, output.get("applicationsSubmitted").asInt());
			}else if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("DAY")) {
				assertEquals(2, output.get("applicationsSubmitted").asInt());
			}else if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("MONTH")) {
				assertEquals(2, output.get("applicationsSubmitted").asInt());
			}else if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("YEAR")) {
				assertEquals(2, output.get("applicationsSubmitted").asInt());
			}	
		}
		consumer.close();
	}

	@Test
	public void testingJsonWithBothUperations() throws JsonMappingException, JsonProcessingException, InterruptedException, ExecutionException {

		Consumer<String,String> consumer=setConsumer(OUTPUT_TOPIC);
		SimpleDateFormat dateFormatter = new SimpleDateFormat(eventPayloadConfigurationYML.getSourceTimeStampFormat());
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = new Date();

		JsonNode node1= mapper.readTree(setJsonValuesForInput("MIDAS",dateFormatter.format(date),"", "APPLICATION_RECEIVED"));
		JsonNode node2= mapper.readTree(setJsonValuesForInput("MIDAS", dateFormatter.format(date),"APPLICATION_RECEIVED", "APPLICATION_APPROVED"));
		JsonNode node3= mapper.readTree(setJsonValuesForInput("MIDAS", dateFormatter.format(date),"APPLICATION_RECEIVED", "APPLICATION_DECLINED"));

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
		TimeUnit.MILLISECONDS.sleep(30000);
		ConsumerRecords<String, String> records=KafkaTestUtils.getRecords(consumer);
		List<JsonNode> receivedOutputs= fetchRecordsFromConsumer(records);
		for(JsonNode output: receivedOutputs) {
			if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("MINUTE")) {
				assertEquals(5, output.get("applicationsSubmitted").asInt());
				assertEquals(2, output.get("applicationsApproved").asInt());
				assertEquals(2, output.get("applicationsPended").asInt());
				assertEquals(1, output.get("applicationsDeclined").asInt());
			}else if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("DAY")) {
				assertEquals(5, output.get("applicationsSubmitted").asInt());
				assertEquals(2, output.get("applicationsApproved").asInt());
				assertEquals(2, output.get("applicationsPended").asInt());
				assertEquals(1, output.get("applicationsDeclined").asInt());
			}else if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("MONTH")) {
				assertEquals(5, output.get("applicationsSubmitted").asInt());
				assertEquals(2, output.get("applicationsApproved").asInt());
				assertEquals(2, output.get("applicationsPended").asInt());
				assertEquals(1, output.get("applicationsDeclined").asInt());
			}else if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("YEAR")) {
				assertEquals(5, output.get("applicationsSubmitted").asInt());
				assertEquals(2, output.get("applicationsApproved").asInt());
				assertEquals(2, output.get("applicationsPended").asInt());
				assertEquals(1, output.get("applicationsDeclined").asInt());
			}	
		}
		consumer.close();
	}

	@Test
	public void testDifferentmetrics() throws JsonMappingException, JsonProcessingException, InterruptedException, ExecutionException {
		Consumer<String,String> consumer=setConsumer(OUTPUT_TOPIC);
		SimpleDateFormat dateFormatter = new SimpleDateFormat(eventPayloadConfigurationYML.getSourceTimeStampFormat());
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.DATE, -1);
		JsonNode node1= mapper.readTree(setJsonValuesForInput("MIDAS", dateFormatter.format(cal.getTime()),"APPLICATION_RECEIVED", "APPLICATION_APPROVED"));
		cal.add(Calendar.MONTH, -1);
		JsonNode node2= mapper.readTree(setJsonValuesForInput("MIDAS", dateFormatter.format(cal.getTime()),"APPLICATION_RECEIVED", "APPLICATION_APPROVED"));
		cal.add(Calendar.YEAR, -1);		
		JsonNode node3= mapper.readTree(setJsonValuesForInput("MIDAS", dateFormatter.format(cal.getTime()),"APPLICATION_RECEIVED", "APPLICATION_APPROVED"));

		logger.info("Testing with application approved events with different applicationSubmittedDates");

		assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,null, node1)).get().hasOffset());
		assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,null, node2)).get().hasOffset());
		assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,null, node3)).get().hasOffset());

		TimeUnit.MILLISECONDS.sleep(30000);
		ConsumerRecords<String, String> records=KafkaTestUtils.getRecords(consumer);
		List<JsonNode> receivedOutputs= fetchRecordsFromConsumer(records);
		for(JsonNode output: receivedOutputs) {
			if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("MINUTE")) {
				assertEquals(3, output.get("applicationsApproved").asInt());
				assertEquals(-3, output.get("applicationsPended").asInt());
			}else if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("DAY")) {
				assertEquals(0, output.get("applicationsApproved").asInt());
				assertEquals(0, output.get("applicationsPended").asInt());
			}else if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("MONTH")) {
				assertEquals(1, output.get("applicationsApproved").asInt());
				assertEquals(-1, output.get("applicationsPended").asInt());
			}else if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("YEAR")) {
				assertEquals(2, output.get("applicationsApproved").asInt());
				assertEquals(-2, output.get("applicationsPended").asInt());
			}	
		}
		consumer.close();
	}

	@Test
	public void testNullEvents() throws InterruptedException, ExecutionException, JsonMappingException, JsonProcessingException {
		Consumer<String,String> consumer=setConsumer(OUTPUT_TOPIC);
		assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,null, null)).get().hasOffset());
		TimeUnit.MILLISECONDS.sleep(30000);
		ConsumerRecords<String, String> records=KafkaTestUtils.getRecords(consumer);
		List<JsonNode> receivedOutputs= fetchRecordsFromConsumer(records);
		assertEquals(4, receivedOutputs.size());
		consumer.close();
	}
	
	@Test
	public void invalidDateEvents() throws JsonMappingException, JsonProcessingException, InterruptedException, ExecutionException {
		Consumer<String,String> consumer=setConsumer(OUTPUT_TOPIC);
		logger.info("Testing with applicationSubmitted, pended apps and 2 records sent");	
		JsonNode node1= mapper.readTree(setJsonValuesForInput("MIDAS","2021-04-02 22:44:556 UTD","", "APPLICATION_RECEIVED"));
		assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,"SampleKey", node1)).get().hasOffset());
		assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,"SampleKey", node1)).get().hasOffset());
		TimeUnit.MILLISECONDS.sleep(30000);	
		ConsumerRecords<String, String> records=KafkaTestUtils.getRecords(consumer);
		List<JsonNode> receivedOutputs= fetchRecordsFromConsumer(records);
		for(JsonNode output: receivedOutputs) {
			if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("MINUTE")) {
				assertEquals(0, output.get("applicationsSubmitted").asInt());
			}else if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("DAY")) {
				assertEquals(0, output.get("applicationsSubmitted").asInt());
			}else if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("MONTH")) {
				assertEquals(0, output.get("applicationsSubmitted").asInt());
			}else if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("YEAR")) {
				assertEquals(0, output.get("applicationsSubmitted").asInt());
			}	
		}
		consumer.close();	
	}

	@Test
	public void testingAccountMetrics() throws InterruptedException, ExecutionException, JsonMappingException, JsonProcessingException {

		Consumer<String,String> consumer=setConsumer(OUTPUT_TOPIC);
		logger.info("Testing with accountopen json, with savings and checking types");	
		JsonNode node1= mapper.readTree(ACCOUNT_OPEN_SAMPLE1);
		JsonNode node2= mapper.readTree(ACCOUNT_OPEN_SAMPLE2);
		assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,"SampleKey", node1)).get().hasOffset());
		assertTrue(producer.send(new ProducerRecord<>(INPUT_TOPIC,"SampleKey", node2)).get().hasOffset());
		TimeUnit.MILLISECONDS.sleep(30000);
		ConsumerRecords<String, String> records=KafkaTestUtils.getRecords(consumer);
		List<JsonNode> receivedOutputs= fetchRecordsFromConsumer(records);
		for(JsonNode output: receivedOutputs) {
			if(output.get(ApplicationMetricsConstants.REFRESHTYPE).asText().equals("MINUTE")) {
				assertEquals(2, output.get("totalAccountsOpened").asInt());
				assertEquals(1, output.get("totalSavingsAccountsOpened").asInt());
				assertEquals(1, output.get("totalCheckingsAccountsOpened").asInt());
			}
		}
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
		SimpleDateFormat dateFormatter = new SimpleDateFormat(eventPayloadConfigurationYML.getSourceTimeStampFormat());
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = new Date();

		JsonNode node1= mapper.readTree(setJsonValuesForInput("MIDAS",dateFormatter.format(date),"APPLICATION_RECEIVED", "APPLICATION_APPROVED"));
		JsonNode node2= mapper.readTree(setJsonValuesForInput("ANOTHER",dateFormatter.format(date), "APPLICATION_RECEIVED", "APPLICATION_APPROVED"));
		assertTrue(appService.filterEvents(eventPayloadConfigurationYML.getFilters(), node1));
		assertTrue(appService.filterEvents(null, node1));
		assertFalse(appService.filterEvents(eventPayloadConfigurationYML.getFilters(), node2));	
	}

	@Test
	public void testApplicationStatusEvaluation() throws JsonMappingException, JsonProcessingException {

		SimpleDateFormat dateFormatter = new SimpleDateFormat(eventPayloadConfigurationYML.getSourceTimeStampFormat());
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = new Date();

		JsonNode node1= mapper.readTree(setJsonValuesForInput("MIDAS", dateFormatter.format(date),"APPLICATION_RECEIVED", "APPLICATION_APPROVED"));
		JsonNode node2= mapper.readTree(setJsonValuesForInput("MIDAS", dateFormatter.format(date),"APPLICATION_RECEIVED", "APPLICATION_APPROVED"));
		JsonNode node3= mapper.readTree(setJsonValuesForInput("MIDAS", dateFormatter.format(date),"APPLICATION_RECEIVED", "APPLICATION_DECLINED"));
		JsonNode node4= mapper.readTree(setJsonValuesForInput("MIDAS",  dateFormatter.format(date),"APPLICATION_RECEIVED", "INVALID_STATUS"));
		JsonNode node5= mapper.readTree(setJsonValuesForInput("MIDAS", dateFormatter.format(date),"INVALID_PREVIOUS_STATUS", "APPLICATION_APPROVED"));
		JsonNode node6= mapper.readTree(setJsonValuesForInput("MIDAS",  dateFormatter.format(date),"INVALID_PREVIOUS_STATUS", "INVALID_STATUS"));
		JsonNode node7= mapper.readTree(setJsonValuesForInput("MIDAS",  dateFormatter.format(date),"APPLICATION_RECEIVED", "APPLICATION_PENDED_ON_ADDITIONAL_DOCS"));
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

	@Test
	public void testFilterDate() throws JsonMappingException, JsonProcessingException {

		SimpleDateFormat dateFormatter = new SimpleDateFormat(eventPayloadConfigurationYML.getSourceTimeStampFormat());
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		JsonNode node= mapper.readTree(setJsonValuesForInput("MIDAS", dateFormatter.format(cal.getTime()),"APPLICATION_RECEIVED", "APPLICATION_APPROVED"));
		cal.add(Calendar.DATE, -1); 
		JsonNode node1= mapper.readTree(setJsonValuesForInput("MIDAS", dateFormatter.format(cal.getTime()),"APPLICATION_RECEIVED", "APPLICATION_APPROVED"));
		cal.add(Calendar.MONTH, -1);
		JsonNode node2= mapper.readTree(setJsonValuesForInput("MIDAS", dateFormatter.format(cal.getTime()),"APPLICATION_RECEIVED", "APPLICATION_APPROVED"));
		cal.add(Calendar.YEAR, -1);
		JsonNode node3= mapper.readTree(setJsonValuesForInput("MIDAS", dateFormatter.format(cal.getTime()),"APPLICATION_RECEIVED", "APPLICATION_APPROVED"));


		assertTrue(appService.filterSubmittedDate("DAY", node, eventPayloadConfigurationYML.getAppSubmittDatePath()));
		assertTrue(appService.filterSubmittedDate("MONTH", node, eventPayloadConfigurationYML.getAppSubmittDatePath()));
		assertTrue(appService.filterSubmittedDate("YEAR", node, eventPayloadConfigurationYML.getAppSubmittDatePath()));
		assertFalse(appService.filterSubmittedDate("UNKNOWNTYPE", node, eventPayloadConfigurationYML.getAppSubmittDatePath()));


		assertFalse(appService.filterSubmittedDate("DAY", node1, eventPayloadConfigurationYML.getAppSubmittDatePath()));
		assertTrue(appService.filterSubmittedDate("MONTH", node1, eventPayloadConfigurationYML.getAppSubmittDatePath()));
		assertTrue(appService.filterSubmittedDate("YEAR", node1, eventPayloadConfigurationYML.getAppSubmittDatePath()));

		assertFalse(appService.filterSubmittedDate("DAY", node2, eventPayloadConfigurationYML.getAppSubmittDatePath()));
		assertFalse(appService.filterSubmittedDate("MONTH", node2, eventPayloadConfigurationYML.getAppSubmittDatePath()));
		assertTrue(appService.filterSubmittedDate("YEAR", node2, eventPayloadConfigurationYML.getAppSubmittDatePath()));

		assertFalse(appService.filterSubmittedDate("DAY", node3, eventPayloadConfigurationYML.getAppSubmittDatePath()));
		assertFalse(appService.filterSubmittedDate("MONTH", node3, eventPayloadConfigurationYML.getAppSubmittDatePath()));
		assertFalse(appService.filterSubmittedDate("YEAR", node3, eventPayloadConfigurationYML.getAppSubmittDatePath()));
	}

	@Test
	public void testJsonTool() throws JsonMappingException, JsonProcessingException {

		final String SAMPLE_INPUT2="{\"event\": {\"header\": {\"name\": \"IIT Add External Accounts\", \"version\": \"1.0\",\"producerCSI\": \"172624\",\"channel\": \"CBOL |  MOB\",\"countryCode\": \"US\",\"businessCode\": \"Retail Services\",\"domain\": \"Pay\",\"uuid\": \"baa83138-b621-4768-90d9-2d87b91bed1d\",\"sid\": \"44d93b64-d446-475b-89cc-f54158fd516f\",\"businessTransactionTime\": \"2020-08-27T02:34:12.00Z\",\"eventTimeStamp\": \"2020-08-27T02:34:12.00Z\"},\"body\": {\"customerIdentifiers\": [{\"customerIdentifier\": \"xxxxxxxx150\",\"customerIdType\": \"CCSID\"},{\"customerIdentifier\": \"xxxxxxxxxX173\",\"customerIdType\": \"CIN\"},{\"customerIdentifier\": \"xxxxxxxxxxx178\",\"customerIdType\": \"BASE_CIN\"}],\"accountIdentifiers\": [],\"eventDetails\": {\"confirmationNumber\": \"084378238147108\",\"accountNumber\": \"xxxxxxxx825\",\"routingNumber\": \"121000248\",\"accountType\": \"Checking\",\"financialInstitutionName\": \"WELLS FARGO BANK, NA\",\"accountNickName\": \"Chase Checking\",\"depositAmount1\": \"0.37\",\"depositAmount2\": \"0.27\",\"responseStatus\": \"Success\",\"transactionTimestamp\": \"2020-08-27T02:34:12.00Z\",\"channelId\": \"CBOl\",\"eventName\": \"IIT_TRIAL_VALIDATE\"}}}}";

		final String SAMPLE_INPUT3="{\"event\": {\"header\": {\"name\": \"IIT Add External Accounts\", \"version\": \"1.0\",\"producerCSI\": \"172624\",\"channel\": \"CBOL |  MOB\",\"countryCode\": \"US\",\"businessCode\": \"Retail Services\",\"domain\": \"Pay\",\"uuid\": \"baa83138-b621-4768-90d9-2d87b91bed1d\",\"sid\": \"44d93b64-d446-475b-89cc-f54158fd516f\",\"businessTransactionTime\": \"2020-08-27T02:34:12.00Z\",\"eventTimeStamp\": \"2020-08-27T02:34:12.00Z\"},\"body\": {\"customerIdentifiers\": [{\"customerIdentifier\": \"xxxxxxxx150\",\"customerIdType\": \"CCSID\"},{\"customerIdentifier\": \"xxxxxxxxxX173\",\"customerIdType\": \"CIN\"},{\"customerIdentifier\": \"xxxxxxxxxxx178\",\"customerIdType\": \"BASE_CIN\"}],\"accountIdentifiers\": [{\"accountIdentifier\": \"xxxxxxxxxx111\",\"accountIdType\": \"accountNumber\"},{\"accountIdentifier\": \"Primary\",\"accountIdType\": \"accountHolderRole\"}],\"eventDetails\": {\"confirmationNumber\": \"084378238147108\",\"accountNumber\": \"xxxxxxxx825\",\"routingNumber\": \"121000248\",\"accountType\": \"Checking\",\"financialInstitutionName\": \"WELLS FARGO BANK, NA\",\"accountNickName\": \"Chase Checking\",\"depositAmount1\": \"0.37\",\"depositAmount2\": \"0.27\",\"responseStatus\": null,\"transactionTimestamp\": \"2020-08-27T02:34:12.00Z\",\"channelId\": \"CBOl\",\"eventName\": \"IIT_TRIAL_VALIDATE\"}}}}";

		JsonNode node1 = mapper.readTree(SAMPLE_INPUT);

		JsonNode node2 = mapper.readTree(SAMPLE_INPUT2);

		JsonNode node3 = mapper.readTree(SAMPLE_INPUT3);

		assertEquals("", JsonTool.fetchString(null, "event.body.eventDetails.responseStatus"));
		assertEquals("", JsonTool.fetchString(node1, "event.body.eventDetails.citiCustomerIdentifierUnknown"));
		assertEquals("TX", JsonTool.fetchString(node1, "event.body.details.address.state"));
		assertEquals("", JsonTool.fetchString(node1, "event.body.details.address.state_2"));
		assertEquals("", JsonTool.fetchString(node2, "event.body.accountIdentifiers.accountIdType"));
		assertEquals("", JsonTool.fetchString(node3, "event.body.eventDetails.responseStatus"));
	}
}

