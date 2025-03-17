package com.o3.server;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRulesException;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.concurrent.locks.ReentrantLock;

public class MessageHandler implements HttpHandler {
	private final MessageDB database;
	private final UserAuthenticator authenticator;
	private final ReentrantLock lock = new ReentrantLock();

	public MessageHandler(MessageDB database, UserAuthenticator authenticator) {
		this.database = database;
		this.authenticator = authenticator;

	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String[] user = new String[2];
		System.out.println("Request handled in thread " + Thread.currentThread().getId());


		JSONArray jsonArray = new JSONArray();
		String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
		if (authHeader != null && authHeader.startsWith("Basic ")) {
			user = Utils.getUSers(authHeader);
		}
		if (Utils.invalidUser(user)) {
			Utils.sendResponse(exchange, 401, "Invalid or non-existent username" + " " +user[0] + " " + user[1]);
			return;
		}

		if ("POST".equals(exchange.getRequestMethod())) {
			JSONObject jsonRequest = null;
			try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
				 BufferedReader bufferedReader = new BufferedReader(reader)) {
				StringBuilder requestBody = new StringBuilder();
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					requestBody.append(line);
				}
				jsonRequest = new JSONObject(requestBody.toString());
				System.out.println();
				handlePost(exchange, user, jsonRequest);
			}catch (IOException e) {
				System.err.println("Error reading request body: " + e.getMessage());
				Utils.sendResponse(exchange, 400, jsonArray.toString());
			} catch (org.json.JSONException e) {
				System.err.println("Invalid JSON format: " + e.getMessage());
				Utils.sendResponse(exchange, 400, jsonArray.toString());

			} catch (SQLException e) {
				e.printStackTrace();
				Utils.sendResponse(exchange, 500, "Database error: " + e.getMessage());
			}
		}
		else if ("GET".equals(exchange.getRequestMethod())) {
			try {
				handleGet(exchange, user);
			} catch (SQLException e) {
				e.printStackTrace();
				Utils.sendResponse(exchange, 500, "Database error: " + e.getMessage());
			}
		} else {
			Utils.sendResponse(exchange, 405, "Method not allowed. Only POST and GET are implemented");
		}
	}

	private void handlePost(HttpExchange exchange, String[] user, JSONObject jsonRequest) throws IOException, SQLException {

		try{
			String recordOwner = jsonRequest.optString("recordOwner", null);
			lock.lock();
			System.out.println("inside Post for recordOwner: " + recordOwner);

			try{
				if(recordOwner == null || recordOwner.isEmpty()){
					recordOwner = database.retrieveNickname(user[0]);
				}
			}finally {
				lock.unlock();
			}
			System.out.println("handling post");
			String recordIdentifier = jsonRequest.getString("recordIdentifier");
			String recordDescription = jsonRequest.getString("recordDescription");
			String recordPayload = jsonRequest.getString("recordPayload");
			String recordRightAscension = jsonRequest.getString("recordRightAscension");
			String recordDeclination = jsonRequest.getString("recordDeclination");

			JSONArray observatoryArray = jsonRequest.optJSONArray("observatory");
			JSONArray observatoryWeatherArray = jsonRequest.optJSONArray("observatoryWeather");
			boolean  success = false;
			System.out.println("Before adding Observatory");
			lock.lock();
			try {
				Observatory observatoryData = null;
				ObservatoryWeather observatoryWeatherData = null;
				if(observatoryArray != null){
					JSONObject observatory = observatoryArray.getJSONObject(0);
					String observatoryName = observatory.getString("observatoryName");
					Double latitude = observatory.getDouble("latitude");
					Double longitude = observatory.getDouble("longitude");
					observatoryData = new Observatory(observatoryName, latitude, longitude);
				}
				if(observatoryWeatherArray != null){
					JSONObject observatoryWeatherObject = observatoryWeatherArray.getJSONObject(0);
					Double temperatureInKelvins = observatoryWeatherObject.getDouble("temperatureInKelvins");
					Double cloudinessPercentance = observatoryWeatherObject.getDouble("cloudinessPercentance");
					Double bagroundLightVolume = observatoryWeatherObject.getDouble("bagroundLightVolume");
					observatoryWeatherData = new ObservatoryWeather(temperatureInKelvins, cloudinessPercentance, bagroundLightVolume);

				}
				Message message = new Message(recordIdentifier, recordDescription, recordPayload,
					recordRightAscension, recordDeclination, ZonedDateTime.now(), recordOwner, observatoryData, observatoryWeatherData);

					success = database.insertObservationRecord(message);

			}finally {
				lock.unlock();
			}
			String response = success ? "Message added successfully" : "Failed to add message";
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			Utils.sendResponse(exchange, success ? 200 : 400, response);
		}catch(Exception e){
			Utils.sendResponse(exchange, 400, "Data does not conform Schema");
		}



	}

	private void handleGet(HttpExchange exchange, String[] user) throws IOException, SQLException {
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
