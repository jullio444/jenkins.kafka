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

import java.util.Map;

public class ExtractDataConstants {

	public static final int ZERO_RETURN_CODE = 0;

	public static final int DEFAULT_ARRAY_GET = 0;
	public static final int CONST_ONE = 1;
	
	public static final String DATA_NOT_AVAILABLE = "DATA_NOT_AVAILABLE";
	public static final String FILE_PREFIX = "file://";
	public static final String DOT = "\\.";
	public static final String STAR = "*";
	
	public static final String CONDITION_ALL = "ALL";
	public static final String CONDITION_ANY = "ANY";
	public static final String CONDITION_FIRST = "FIRST";
	public static final String CONDITION_LAST = "LAST";
	
	public static final String CONST_CONDITIONS = "conditions";
	public static final String CONST_FILTER_TYPE = "filterType";
	public static final String CONST_FIELD = "field";
	public static final String CONST_VALUE = "value";
	public static final String CONST_SATISFY_CONDITION = "“satisfyCondition”";
	
	public static final Map<String,String> MAP_NULL = null;
	public static final String STRING_NULL = null;
	public static final String STRING_EMPTY = "";
	
	private ExtractDataConstants() {}
	
}
