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
package com.citi.gcg.eventhub.midas.exception;

/***
 * 
 * A custom exception class to throw an exception in EventPayloadConfigurationYML class
 * 
 *
 *
 */
public class MetricsApplicationException extends Exception{

	private static final long serialVersionUID = 1L;

	public MetricsApplicationException(String message) {
		super(message);
	}
	
	public MetricsApplicationException(String message, Throwable throwable) {
		super(message, throwable);
	}
	
}