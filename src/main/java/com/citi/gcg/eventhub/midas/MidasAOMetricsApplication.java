
package com.citi.gcg.eventhub.midas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/***
 * 
 * As part of MIDAS AO real-time Metrics Reporting, following application is built.
 * Applications is built to support the BAU requirements too in case any similar use-case is requested. 
 * 
 *
 *
 */
@SpringBootApplication
public class MidasAOMetricsApplication {

	/***
	 * Application main method to start and process the events.
	 */
	public static void main(String[] args) {
		SpringApplication.run(MidasAOMetricsApplication.class, args);
	}
}
