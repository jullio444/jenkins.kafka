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
package com.citi.gcg.eventhub.midas.constants;

public class ApplicationMetricsConstants {

	public static final String KEY_SERDE = "org.apache.kafka.common.serialization.Serdes$StringSerde";
	public static final String VALUE_SERDE = "com.citi.gcg.eventhub.midas.kafka.serde.JsonSerde";

	public static final  String CURRENT_APPLICATION_STATUS = "applicationStatus";
	public static final  String PREVIOUS_APPLICATION_STATUS = "previousApplicationStatus";
	public static final  String APPLICATION_OPERATION = "applicationOperation";
	public static final  String APPLICATION_OPERATION_NEW = "I";
	public static final  String APPLICATION_OPERATION_UPDATE = "U";
	
	
	
	public static final  String UNDEFINEDTYPE="UndefinedType";
	public static final  String UNDEFINEDSTATUS="UndefinedStatus";
	
	public static final String AGGREGATOR_STATSTORE = "aggregator-statestore";
	public static final String TRANSFORMER_STATSTORE = "transformer-statestore";
	public static final String APPID_STATSTORE = "applicacionId-statestore";
	
	public static final long CONTEXT_SCHEDULE = 1;
	
	public static final String AGGREGATOR_APPROVED = "applications_approved";
	public static final String AGGREGATOR_PENDED = "applications_pended";
	public static final String AGGREGATOR_DECLINED = "applications_declined";
	public static final String AGGREGATOR_SUBMITTED = "applications_submitted";

	public static final String TOTAL_APPLICATIONS = "applicationsSubmitted";
	public static final String TOTAL_APPROVED = "applicationsApproved";
	public static final String TOTAL_PENDED = "applicationsPended";
	public static final String TOTAL_DECLINED = "applicationsDeclined";
	
	private ApplicationMetricsConstants() {}
	
}
