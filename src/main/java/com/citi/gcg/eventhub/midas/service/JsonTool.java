package com.citi.gcg.eventhub.midas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

/***
 * This class allows to work with Jsons easily
 * It was removed the interaction with Java Reflexion
 * @author EventHub Dev
 *
 */
public class JsonTool {

	private static final Logger LOG = LoggerFactory.getLogger(JsonTool.class);

	private JsonTool() {
		throw new IllegalStateException("JsonTool utility class");
	}

	public static JsonNode fetchNode(JsonNode node, String path) {
		String[] keys = path.split("\\.");
		JsonNode aux = node;
		for (String key : keys) {
			if (aux == null) {
				LOG.info("Unable to find the Path: {}", path);
				return null;
			}
			// Case if the json is a Json Array
			if (aux.isArray()) {
				if (aux.size() > 0) {
					// *Fetch The first element
					aux=fetchDefaultFirstElement(aux);

				} else {
					return null;
				}
			}
			// end of the case when the field is a json array
			try {
				aux = aux.get(key);
			} catch (NullPointerException e) {
				LOG.error(String.format("Unable to extract the value from %s, the field: %s is not there.", path, key));
				return null;
			}
		}
		return aux;
	}
	
	private static JsonNode fetchDefaultFirstElement(JsonNode aux) {
		JsonNode tempNode=null;
		for (JsonNode internalNode : aux) {
			tempNode = internalNode;
			if (tempNode != null) {
				aux = tempNode;
				break;
			}
		}
		return aux;
	}
	public static String fetchString(JsonNode node, String path) {
		JsonNode targetNode = fetchNode(node, path);
		return targetNode != null ? targetNode.asText("") : "";
	}

}
