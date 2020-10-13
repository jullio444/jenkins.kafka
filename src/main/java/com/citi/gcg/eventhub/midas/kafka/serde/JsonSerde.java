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
package com.citi.gcg.eventhub.midas.kafka.serde;

import org.apache.kafka.common.serialization.Serdes.WrapperSerde;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;

import com.fasterxml.jackson.databind.JsonNode;
/***
 * Custom Json Serde to create Serde (SerializerDeserializer) for JSON.
 * 
 * @author EventHub Dev Team
 *
 */
public class JsonSerde extends WrapperSerde<JsonNode>{

	public JsonSerde() {
		super(new JsonSerializer(), new JsonDeserializer());	
	}
	
}