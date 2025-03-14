package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONObject;
import org.json.JSONException;

public class RegistrationHandler implements HttpHandler {

	private final UserAuthenticator userAuth;
	private final ReentrantLock lock = new ReentrantLock();

	public RegistrationHandler(UserAuthenticator userAuth) {
		this.userAuth = userAuth;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		System.out.println("Request handled in thread " + Thread.currentThread().getId());
		if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
			sendResponse(exchange, 405, "Method not supported. Only POST is supported.");
			return;
		}
		handlePost(exchange);
	}

	private void handlePost(HttpExchange exchange) throws IOException {
		try {
			String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
			if (contentType == null || !contentType.equalsIgnoreCase("application/json")) {
				sendResponse(exchange, 400, "Unsupported Content Type. Only application/json is supported.");
				return;
			}

			StringBuilder requestBodyBuilder = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					requestBodyBuilder.append(line);
				}
			}

			String requestBody = requestBodyBuilder.toString();
			if (requestBody.isEmpty()) {
				sendResponse(exchange, 400, "Request body is empty.");
				return;
			}

			JSONObject jsonObject;
			try {
				jsonObject = new JSONObject(requestBody);
			} catch (JSONException e) {
				sendResponse(exchange, 400, "Invalid JSON format!");
				System.err.println("JSON Parsing Error: " + e.getMessage());
				return;
			}

			String username = jsonObject.optString("username", "").trim();
			String password = jsonObject.optString("password", "").trim();
			String email = jsonObject.optString("email", "").trim();
			String userNickname = jsonObject.optString("userNickname", "").trim();

			if (username.isEmpty() || password.isEmpty() || email.isEmpty() || userNickname.isEmpty()) {
				sendResponse(exchange, 400, "Missing required fields: username, password, email, or userNickname.");
				return;
			}

			boolean isRegistered;
			lock.lock();
			try {
				isRegistered = userAuth.addUser(username, password, email, userNickname);
			} finally {
				lock.unlock();
			}

			if (isRegistered) {
				sendResponse(exchange, 200, "Registration Successful!");
			} else {
				sendResponse(exchange, 409, "User already exists!");
			}
		} catch (SQLException e) {
			System.err.println("Database Error: " + e.getMessage());
			sendResponse(exchange, 500, "Internal Server Error");
		} catch (Exception e) {
			System.err.println("Unexpected Error: " + e.getMessage());
			sendResponse(exchange, 500, "An unexpected error occurred.");
		} finally {
			exchange.close();
		}
	}

	private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
		byte[] response = message.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(statusCode, response.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(response);
		}
	}
}
