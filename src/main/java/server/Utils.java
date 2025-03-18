package server;


import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.net.URL;


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

	protected static boolean  invalidUser(String[] user, UserAuthenticator authenticator ) {
		if(user[0] == null || user[1] == null || !authenticator.checkCredentials(user[0], user[1])){
			return true;
		}
		return false;
	}

	protected  static String sendRequest(String endpointUrl) throws Exception {
		URL url = new URL(endpointUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Accept", "application/xml");
		if(connection.getResponseCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + connection.getResponseCode());
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		StringBuilder respose = new StringBuilder();
		String output;
		while ((output = br.readLine()) != null) {
			respose.append(output);
		}
		return respose.toString();

	}

	protected static Double[] extractParameters(JSONObject response) {
		JSONObject featureCollection = response.getJSONObject("wfs:FeatureCollection");
		JSONArray members = featureCollection.getJSONArray("wfs:member");
		Double[] parametervalues = new Double[3];

		for(int i = 0; i < members.length(); i++) {
			JSONObject member = members.getJSONObject(i).getJSONObject("BsWfs:BsWfsElement");
			String parameterName = member.getString("BsWfs:ParameterName");
			double parameterValue = member.getDouble("BsWfs:ParameterValue");

			switch (parameterName) {
				case "temperatureInKelvins":
					parametervalues[0] = parameterValue;
					break;
				case "cloudinessPercentance":
					parametervalues[1] = parameterValue;
					break;
				case "bagroundLightVolume":
					parametervalues[2] = parameterValue;
					break;
				default:
					System.out.println("Unexpected parameter name: " + parameterName);
					break;
			}

		}
		System.out.println("Parameter values return: " + Arrays.toString(parametervalues));
		return parametervalues;
	}
}

