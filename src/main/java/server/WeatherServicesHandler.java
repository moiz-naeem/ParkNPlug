package server;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

public class WeatherServicesHandler implements HttpHandler {
	private final MessageDB database;
	private final UserAuthenticator authenticator;
	private final ReentrantLock lock = new ReentrantLock();

	public WeatherServicesHandler(MessageDB database, UserAuthenticator authenticator) {
		this.database = database;
		this.authenticator = authenticator;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		System.out.println("WeatherService handled in thread " + Thread.currentThread().getId());
		String[] user = new String[2];

		JSONArray jsonArray = new JSONArray();
		String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
		if (authHeader != null && authHeader.startsWith("Basic ")) {
			user = Utils.getUSers(authHeader);
		}
		if (Utils.invalidUser(user)) {
			Utils.sendResponse(exchange, 401, "Invalid or non-existent username" + " " +user[0] + " " + user[1]);
			return;
		}


	}


}
