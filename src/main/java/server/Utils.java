package server;


import com.sun.net.httpserver.HttpExchange;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


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

	protected  static String formUrlEncode(Double latitude, Double longitude) {
		String Url = String.format(
			"http://localhost:4001/wfs?latlon=%.2f,%.2f&parameters=temperatureInKelvins,cloudinessPercentance,bagroundLightVolume&starttime=%s",
			latitude, longitude, DateTimeFormatter.ISO_INSTANT.format(Instant.now())
		);
		return Url;
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

	protected static Map<String, String> parseQueryString(String queryString){
		Map<String, String> parameters = new HashMap<>();
		String[] pairs = queryString.split("&");
		System.out.println("Pairs: " + Arrays.toString(pairs));
		for(String pair : pairs){
			String[] idx = pair.split("=", 2);
			System.out.println("idx: " + Arrays.toString(idx));
			String key = URLDecoder.decode(idx[0], StandardCharsets.UTF_8);
			String value = idx.length > 1 ? URLDecoder.decode(idx[1], StandardCharsets.UTF_8) : "";
			System.out.println("key: " + key + " value: " + value);
			parameters.put(key, value);
		}
		System.out.println(parameters);
		return parameters;
	}
	protected static JSONArray extractMessage(PreparedStatement statement) {

		JSONArray errorResponse = new JSONArray();
		JSONArray messages = new JSONArray();
		try (ResultSet resultSet = statement.executeQuery()) {
			try {
				while (resultSet.next()) {
					String updateReason =  resultSet.getObject("updateReason") != null ? resultSet.getString("updatereason") : "";
					try {
						DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
							.withZone(ZoneOffset.UTC);
						Message message = new Message(
							resultSet.getString("recordIdentifier"),
							resultSet.getString("recordDescription"),
							resultSet.getString("recordPayload"),
							resultSet.getString("recordRightAscension"),
							resultSet.getString("recordDeclination"),
							resultSet.getLong("recordTimeReceived"),
							resultSet.getString("recordOwner"),
							null,
							null,
							updateReason,
							resultSet.getString("messagePoster")
						);

						JSONObject json = new JSONObject();
						json.put("id", resultSet.getInt("id"));
						json.put("recordIdentifier", message.getRecordIdentifier());
						json.put("recordDescription", message.getRecordDescription());
						json.put("recordPayload", message.getRecordPayload());
						json.put("recordRightAscension", message.getRecordRightAscension());
						json.put("recordDeclination", message.getRecordDeclination());
						json.put("recordOwner", message.getRecordOwner());
						json.put("recordTimeReceived", message.getRecordTimeReceived());
						json.put("updateReason", updateReason.isEmpty() ? null : updateReason);
						json.put("modified", updateReason.isEmpty() ? null : formatter.format(Instant.ofEpochMilli(resultSet.getLong("modified"))));

						JSONArray observatoryArray = new JSONArray();
						String observatoryName = resultSet.getString("observatoryName");
						Double latitude = resultSet.getObject("latitude") != null ? resultSet.getDouble("latitude") : null;
						Double longitude = resultSet.getObject("longitude") != null ? resultSet.getDouble("longitude") : null;
						if (observatoryName != null || latitude != null || longitude != null) {
							JSONObject observatoryJson = new JSONObject();
							observatoryJson.put("observatoryName", observatoryName);
							observatoryJson.put("latitude", latitude);
							observatoryJson.put("longitude", longitude);
							observatoryArray.put(observatoryJson);
							json.put("observatory", observatoryArray);

						}


						JSONArray weatherArray = new JSONArray();
						Double temperatureInKelvins = resultSet.getObject("temperatureInKelvins") != null ? resultSet.getDouble("temperatureInKelvins") : null;
						Double cloudinessPercentance = resultSet.getObject("cloudinessPercentance") != null ? resultSet.getDouble("cloudinessPercentance") : null;
						Double bagroundLightVolume = resultSet.getObject("bagroundLightVolume") != null ? resultSet.getDouble("bagroundLightVolume") : null;
						if (temperatureInKelvins != null || cloudinessPercentance != null || bagroundLightVolume != null) {
							JSONObject weatherJson = new JSONObject();
							weatherJson.put("temperatureInKelvins", temperatureInKelvins);
							weatherJson.put("cloudinessPercentance", cloudinessPercentance);
							weatherJson.put("bagroundLightVolume", bagroundLightVolume);
							weatherArray.put(weatherJson);
							json.put("observatoryWeather", weatherArray);

						}

						messages.put(json);


					} catch (JSONException e) {
						System.out.println("JSONException: " + e.getMessage());
						return errorResponse;
					}
				}
			} catch (SQLException e) {
				System.out.println("SQLException: " + e.getMessage());
				return errorResponse;
			}
			return messages;

		}catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
			return errorResponse;
		}

	}

}

