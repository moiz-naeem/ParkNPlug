

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.XML;

public class WeatherServices {
	private static final String WEATHER_API_URL =
		"http://localhost:4001/wfs?latlon={latitude},{longitude}&parameters=temperatureInKelvins,cloudinessPercentance,bagroundLightVolume";
	private static final int PRETTY_PRINT_INDENT_FACTOR = 4;

	public WeatherData getWeatherData(double latitude, double longitude) throws Exception {
		String url = WEATHER_API_URL.replace("{latitude}", String.valueOf(latitude))
			.replace("{longitude}", String.valueOf(longitude));

		HttpURLConnection connection = null;
		try {
			// Open connection
			connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(5000); // 5 seconds
			connection.setReadTimeout(5000);    // 5 seconds

			int responseCode = connection.getResponseCode();
			if (responseCode == 200) {
				// Read the XML response
				BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				StringBuilder response = new StringBuilder();
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();

				// Convert XML to JSON
				JSONObject xmlJSONObj = XML.toJSONObject(response.toString());
				String jsonPrettyPrintString = xmlJSONObj.toString(PRETTY_PRINT_INDENT_FACTOR);
				System.out.println("Converted JSON: \n" + jsonPrettyPrintString);

				// Map the JSON to WeatherData
				return parseWeatherData(xmlJSONObj);
			} else {
				throw new RuntimeException("API call failed. HTTP Response Code: " + responseCode);
			}
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private WeatherData parseWeatherData(JSONObject xmlJSONObj) {
		// Extract values from the JSON structure to populate WeatherData
		WeatherData weatherData = new WeatherData();
		JSONObject featureCollection = xmlJSONObj.getJSONObject("FeatureCollection");
		JSONArray members = featureCollection.getJSONArray("member");

		for (int i = 0; i < members.length(); i++) {
			JSONObject member = members.getJSONObject(i).getJSONObject("BsWfsElement");
			String parameterName = member.getString("ParameterName");
			double parameterValue = member.getDouble("ParameterValue");

			switch (parameterName) {
				case "temperatureInKelvins":
					weatherData.setTemperatureInKelvins(parameterValue);
					break;
				case "cloudinessPercentance":
					weatherData.setCloudinessPercentance(parameterValue);
					break;
				case "bagroundLightVolume":
					weatherData.setBagroundLightVolume(parameterValue);
					break;
				default:
					throw new IllegalArgumentException("Unexpected parameter: " + parameterName);
			}
		}

		return weatherData;
	}
}
