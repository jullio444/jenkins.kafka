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

/***
 * Application Account opening Constants Class for processor class
 * 
 * @author EventHub Dev Team
 *
 */

public class AppAOConstants {

	public static final String METRICS_APP_SUBMITTED = "applicationsSubmitted";
	public static final String METRICS_APP_APPROVED = "applicationsApproved";
	public static final String METRICS_APP_PENDED = "applicationsPended";
	public static final String METRICS_APP_DECLINED = "applicationsDeclined";
	public static final String TRANSACTIONDATETIME = "transactionDateTime";
	public static final String METRICS_TOTAL_ACCOUNTS = "totalAccountsOpened";
	public static final String METRICS_SAVINGS_ACCOUNTS = "totalSavingsAccountsOpened";
	public static final String METRICS_CHECKING_ACCOUNTS = "totalCheckingsAccountsOpened";
	public static final String METRIC= "metric";

	private AppAOConstants() {}
	
}