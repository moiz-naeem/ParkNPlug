package server;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRulesException;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.concurrent.locks.ReentrantLock;
import java.time.format.DateTimeFormatter;
import java.net.HttpURLConnection;
import java.net.URL;

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
		if (Utils.invalidUser(user, authenticator)) {
			Utils.sendResponse(exchange, 401, "Invalid or non-existent username" + " " +user[0] + " " + user[1]);
			return;
		}

		if ("POST".equals(exchange.getRequestMethod())) {
			JSONObject jsonRequest = null;
			String username = user[0];
			try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
				 BufferedReader bufferedReader = new BufferedReader(reader)) {
				StringBuilder requestBody = new StringBuilder();
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					requestBody.append(line);
				}
				System.out.println("request body: " + requestBody.toString());
				jsonRequest = new JSONObject(requestBody.toString());
//				jsonRequest.put("username", username);
				System.out.println();
				Handler.handlePost(exchange, user, jsonRequest, database);
			}catch (IOException e) {
				System.err.println("Error reading request body: " + e.getMessage());
				Utils.sendResponse(exchange, 400, jsonArray.toString());
			} catch (JSONException e) {
				System.err.println("Invalid JSON format: " + e.getMessage());
				Utils.sendResponse(exchange, 400, jsonArray.toString());

			} catch (SQLException e) {
				e.printStackTrace();
				Utils.sendResponse(exchange, 500, "Database error: " + e.getMessage());
			}
		}
		else if ("GET".equals(exchange.getRequestMethod())) {
			try {
				Handler.handleGet(exchange, user, database);
			} catch (SQLException e) {
				e.printStackTrace();
				Utils.sendResponse(exchange, 500, "Database error: " + e.getMessage());
			}
		}
		else if ("PUT".equals(exchange.getRequestMethod())) {
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
				Handler.handlePut(exchange, user, jsonRequest, database);
			}catch (SQLException e){
				e.printStackTrace();
				Utils.sendResponse(exchange, 500, "Database error : " + e.getMessage());
			}

		}
		 else {
			Utils.sendResponse(exchange, 405, "Method not allowed. Only POST and GET are implemented");
		}
	}





}
