package com.o3.server;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class Handler{
	private static final ReentrantLock lock = new ReentrantLock();

	protected static void handlePut(HttpExchange exchange, String[] user, JSONObject jsonRequest, MessageDB database) throws IOException, SQLException {
		try{
			URI requestUri = exchange.getRequestURI();
			String query = requestUri.getRawQuery();
			System.out.println("Request URI: " + requestUri);
			System.out.println("Query string: " + query);
			if(query == null) {
				Utils.sendResponse(exchange, 400, "No query parameters found");
				System.out.println("Response: No query parameters found" );
				return;
			}
			Map<String, String> queryParams = Utils.parseQueryString(query);
			if(queryParams.size() == 1 && queryParams.containsKey("id")) {
				System.out.println("Size: " + queryParams.size());
				System.out.println("id: " + queryParams.get("id"));
				System.out.println("Query parameters: " + queryParams);

				int statusCode = database.updateMessageData(Integer.parseInt(queryParams.get("id")), jsonRequest, user[0]);

                String response =  statusCode == 200 ? "Updated" : "NOT Updated";
				System.out.println("Response: " + response + " Status: " + statusCode);
				exchange.getResponseHeaders().add("Content-Type", "application/json");
				Utils.sendResponse(exchange, statusCode, response);
			}else{
				Utils.sendResponse(exchange, 400, "Parameters exceed limit or id is missing ");


			}


			// query = java.net.URLDecoder.decode(query, StandardCharsets.UTF_8.name());
			// String recordIdentifier = query.split("=")[1];

			// JSONArray jsonResponse = database.retrieveObservationRecord(recordIdentifier);
//			JSONArray jsonResponse = new JSONArray();
//			lock.lock();
//			try{
//				jsonResponse = database.retrieveObservationRecord();
//
//			}finally{
//				lock.unlock();
//			}
//			exchange.getResponseHeaders().add("Content-Type", "application/json");
//			if (jsonResponse.length() > 0) {
//				Utils.sendResponse(exchange, 200, jsonResponse.toString());
//			} else {
//
//				errorObject.put("error", "No records found for identifier");
//				errorResponse.put(errorObject);
//				Utils.sendResponse(exchange, 404, errorResponse.toString());
//			}
		}catch (Exception e) {
			System.err.println("Database error: " + e.getMessage());
			JSONArray errorResponse = new JSONArray();
			JSONObject errorObject = new JSONObject();
			errorObject.put("error", "Database error");
			errorResponse.put(errorObject);
			Utils.sendResponse(exchange, 500, errorResponse.toString());
		}
//		 catch (IOException e) {
//
//
//			System.err.println("IO error: " + e.getMessage());
//			JSONArray errorResponse = new JSONArray();
//			JSONObject errorObject = new JSONObject();
//			errorObject.put("error", "IO error");
//			errorResponse.put(errorObject);
//			Utils.sendResponse(exchange, 500, errorResponse.toString());
//		}
	}
	protected static void handlePost(HttpExchange exchange, String[] user, JSONObject jsonRequest, MessageDB database) throws IOException, SQLException {
		try {
			System.out.println("User Payload: " + jsonRequest.toString());
			String recordOwner = jsonRequest.optString("recordOwner", null);
			lock.lock();
			System.out.println("Inside POST for recordOwner: " + recordOwner);

			try {
				if (recordOwner == null || recordOwner.isEmpty()) {
					recordOwner = database.retrieveNickname(user[0]);
				}
			} finally {
				lock.unlock();
			}

			System.out.println("Handling POST request");

			String recordIdentifier = jsonRequest.getString("recordIdentifier");
			String recordDescription = jsonRequest.getString("recordDescription");
			String recordPayload = jsonRequest.getString("recordPayload");
			String recordRightAscension = jsonRequest.getString("recordRightAscension");
			String recordDeclination = jsonRequest.getString("recordDeclination");
			String messagePoster = user[0];

			JSONArray observatoryArray = jsonRequest.optJSONArray("observatory");
			JSONArray observatoryWeatherArray = jsonRequest.optJSONArray("observatoryWeather");
			boolean success = false;

			System.out.println("Before adding Observatory");
			lock.lock();
			try {
				Observatory observatoryData = null;
				ObservatoryWeather observatoryWeatherData = null;

				if (observatoryArray != null) {
					JSONObject observatory = observatoryArray.getJSONObject(0);

					if (observatory.length() < 3) {
						System.out.println("Observatory is missing fields");
						Utils.sendResponse(exchange, 400, "Observatory is missing fields");
						return;
					}

					String observatoryName = observatory.getString("observatoryName");
					Double latitude = observatory.getDouble("latitude");
					Double longitude = observatory.getDouble("longitude");

					observatoryData = new Observatory(observatoryName, latitude, longitude);
				}

				if (observatoryWeatherArray != null && observatoryWeatherArray.isEmpty()) {
					if (observatoryArray == null || observatoryArray.getJSONObject(0).length() < 3) {
						System.out.println("Observatory data is missing for fetching weather data");
						Utils.sendResponse(exchange, 400, "Observatory data is missing");
						return;
					}

					JSONObject observatory = observatoryArray.getJSONObject(0);


					Double latitude = observatory.getDouble("latitude");
					Double longitude = observatory.getDouble("longitude");
					String endpointUrl = Utils.formUrlEncode(latitude, longitude);

					System.out.println("Endpoint: " + endpointUrl);

					String xmlResponse = Utils.sendRequest(endpointUrl);
					JSONObject jsonResponse = XML.toJSONObject(xmlResponse);
					System.out.println("XML to JSON: " + jsonResponse);

					Double[] parameterValues = Utils.extractParameters(jsonResponse);

					observatoryWeatherData = new ObservatoryWeather(parameterValues[0], parameterValues[1], parameterValues[2]);
				}

				Message message = new Message(
					recordIdentifier, recordDescription, recordPayload,
					recordRightAscension, recordDeclination, ZonedDateTime.now(),
					recordOwner, observatoryData, observatoryWeatherData, null, messagePoster
				);

				System.out.println("Final message to be added: " + message);

				success = database.insertObservationRecord(message);
			} finally {
				lock.unlock();
			}

			String response = success ? "Message added successfully" : "Failed to add message";
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			Utils.sendResponse(exchange, success ? 200 : 400, response);
		} catch (Exception e) {

			System.err.println("Error processing POST request: " + e.getMessage());
			Utils.sendResponse(exchange, 400, "Data does not conform to schema: " + e.getMessage());
		}
	}
	protected static void handleGet(HttpExchange exchange, String[] user, MessageDB database) throws IOException, SQLException {
		try{
			JSONArray errorResponse = new JSONArray();
			JSONObject errorObject = new JSONObject();


			// String query = exchange.getRequestURI().getQuery();
			// if (query == null || !query.startsWith("recordIdentifier=") || !query.contains("=")) {
			//     errorObject.put("error", "Bad Request or incorrect query parameter");
			//     errorResponse.put(errorObject);
			//     sendResponse(exchange, 400,  errorResponse.toString());
			//     return;
			// }

			// query = java.net.URLDecoder.decode(query, StandardCharsets.UTF_8.name());
			// String recordIdentifier = query.split("=")[1];

			// JSONArray jsonResponse = database.retrieveObservationRecord(recordIdentifier);
			JSONArray jsonResponse = new JSONArray();
			lock.lock();
			try{
				jsonResponse = database.retrieveObservationRecord();

			}finally{
				lock.unlock();
			}
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			if (jsonResponse.length() > 0) {
				Utils.sendResponse(exchange, 200, jsonResponse.toString());
			} else {

				errorObject.put("error", "No records found for identifier");
				errorResponse.put(errorObject);
				Utils.sendResponse(exchange, 404, errorResponse.toString());
			}
		}catch (SQLException e) {
			System.err.println("Database error: " + e.getMessage());
			JSONArray errorResponse = new JSONArray();
			JSONObject errorObject = new JSONObject();
			errorObject.put("error", "Database error");
			errorResponse.put(errorObject);
			Utils.sendResponse(exchange, 500, errorResponse.toString());
		} catch (IOException e) {
			System.err.println("IO error: " + e.getMessage());
			JSONArray errorResponse = new JSONArray();
			JSONObject errorObject = new JSONObject();
			errorObject.put("error", "IO error");
			errorResponse.put(errorObject);
			Utils.sendResponse(exchange, 500, errorResponse.toString());
		}
	}

}
