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

public class AppYmlConfigConstants {

	public static final String EVENT_PAYLOAD_YML_PREFIX = "eventhub.payload.metrics.conditions";
	public static final String EVENT_KAFKA_YML_PREFIX = "eventhub.configuration.kstreams";
	public static final String OUTPUT_CONFIG_YML_PREFIX= "config.output";

	public static final String FILE_PREFIX = "file://";

	public static final String CONST_CONDITIONS = "conditions";
	public static final String CONST_FILTERS = "filters";
	public static final String CONST_FILTER_TYPE = "filterType";
	public static final String CONST_FIELD = "field";
	public static final String CONST_VALUE = "value";

	public static final String CONDITION_ALL = "ALL";
	public static final String CONDITION_ANY = "ANY";

	private AppYmlConfigConstants() {}

}
