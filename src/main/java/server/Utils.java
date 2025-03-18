


import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Utils {

	protected static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
		exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
		try (OutputStream outputStream = exchange.getResponseBody()) {
			outputStream.write(response.getBytes(StandardCharsets.UTF_8));
		}
	}

	protected static String[] getUSers(String authHeader) {
		String base64Credentials = authHeader.substring("Basic ".length());
		String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
		String[] split = credentials.split(":", 2);
		return new String[] { split[0], split[1] };
	}

	protected static boolean  invalidUser(String[] user ) {
		if(user[0] == null || user[1] == null || !authenticator.checkCredentials(user[0], user[1])){
			return true;
		}
		return false;
	}
}

