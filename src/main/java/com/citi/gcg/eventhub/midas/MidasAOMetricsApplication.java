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
package com.citi.gcg.eventhub.midas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/***
 * 
 * As part of MIDAS AO real-time Metrics Reporting, following application is built.
 * Applications is built to support the BAU requirements too in case any similar use-case is requested. 
 * 
 * @author EventHub Dev Team
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
