package server;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class SearchHandler implements HttpHandler {
	private final MessageDB database;
	private final UserAuthenticator authenticator;
	private final ReentrantLock lock = new ReentrantLock();

	public SearchHandler(MessageDB database, UserAuthenticator authenticator) {
		this.database = database;
		this.authenticator = authenticator;

	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String[] user = new String[2];
		try {
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			System.out.println("Request handled in thread " + Thread.currentThread().getId());
			String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
			if (authHeader != null && authHeader.startsWith("Basic ")) {
				user = Utils.getUSers(authHeader);
			}
			if (Utils.invalidUser(user, authenticator)) {
				Utils.sendResponse(exchange, 401, "Invalid or non-existent username" + " " + user[0] + " " + user[1]);
				return;
			}
			if ("GET".equals(exchange.getRequestMethod())) {
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
				System.out.println("Query parameters: " + queryParams);

				JSONArray responseArray = new JSONArray();
				lock.lock();
				try {
					responseArray = database.retrieveParamerterizedObservationRecords(queryParams);
				} finally {
					lock.unlock();
				}
				if (responseArray.length() > 0) {
					Utils.sendResponse(exchange, 200, responseArray.toString());
					System.out.println("Response: " + responseArray);
				} else {
					Utils.sendResponse(exchange, 200, responseArray.toString());
					System.out.println("Response: " + responseArray);

				}



			} else {
				Utils.sendResponse(exchange, 400, "Method not supported ");
			}
		} catch (Exception e) {
			Utils.sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
		}

	}
}
