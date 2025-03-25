package server;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.security.SecureRandom;
import java.util.*;

import org.apache.commons.codec.digest.Crypt;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.locks.ReentrantLock;

public class MessageDB {
	private Connection dbConnection = null;
	private static MessageDB dbInstance = null;
	private SecureRandom secureRandom = new SecureRandom();
    private final ReentrantLock lock = new ReentrantLock();

	public static synchronized MessageDB getInstance() {
		if (dbInstance == null) {
			dbInstance = new MessageDB();
		}
		return dbInstance;
	}

	private MessageDB() {}

	public void open(String dbName) throws SQLException {
		lock.lock();
		try {

			File dbFile = new File(dbName);
			boolean needsInitialisation = !dbFile.exists() || dbFile.isDirectory();
			Class.forName("org.sqlite.JDBC");
			String database = "jdbc:sqlite:" + dbName;
			dbConnection = DriverManager.getConnection(database);

			if (needsInitialisation) {
				initializeDatabase();
			}
		} catch (ClassNotFoundException e) {
			System.err.println("SQLite JDBC driver not found: " + e.getMessage());
		}finally {
			lock.unlock();
		}
	}

	private boolean initializeDatabase() throws SQLException {
		lock.lock();
		try{
			if (!isConnectionValid()) {
				throw new SQLException("Database connection is not valid.");
			}else{
				String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
					+ "username VARCHAR(50) NOT NULL PRIMARY KEY,"
					+ "password VARCHAR(50) NOT NULL,"
					+ "email VARCHAR(50) NOT NULL UNIQUE,"
					+ "userNickname VARCHAR(50) NOT NULL"
					+ ")";

				String createObservatoryTable = "CREATE TABLE IF NOT EXISTS observatory ("
					+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "observatoryName VARCHAR(100) NULL,"
					+ "latitude DOUBLE NULL,"
					+ "longitude DOUBLE NULL,"
					+ "temperatureInKelvins DOUBLE NULL,"
					+ "cloudinessPercentance DOUBLE NULL,"
					+ "bagroundLightVolume DOUBLE NULL"
					+ ")";

				String createMessagesTable = "CREATE TABLE IF NOT EXISTS messages ("
					+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "recordIdentifier VARCHAR(50) NOT NULL UNIQUE,"
					+ "recordDescription TEXT NOT NULL,"
					+ "recordPayload TEXT NOT NULL,"
					+ "recordRightAscension VARCHAR(50) NOT NULL,"
					+ "recordDeclination VARCHAR(50) NOT NULL,"
					+ "recordTimeReceived BIGINT NOT NULL,"
					+ "modified BIGINT NULL,"
					+ "recordOwner VARCHAR(50),"
					+ "observatoryId INTEGER NULL,"
					+ "updatereason TEXT NULL,"
					+ "messagePoster VARCHAR(50),"
					+ "FOREIGN KEY (observatoryId) REFERENCES Observatory(id)"
					+ ")";


				try (Statement createStatement = dbConnection.createStatement()) {
					createStatement.executeUpdate(createUsersTable);
					createStatement.executeUpdate(createMessagesTable);
					createStatement.executeUpdate(createObservatoryTable);
					return true;
				}catch (SQLException e) {
					System.err.println("Error initializing database: " + e.getMessage());
				}
			}
		}finally {
			lock.unlock();
		}
		return false;



	}
	private boolean isConnectionValid() {
		try {
			return dbConnection != null && !dbConnection.isClosed();
		} catch (SQLException e) {
			return false;
		}
	}


	public void closeDB() throws SQLException {
		lock.lock();
		try{
			if (dbConnection != null) {
				dbConnection.close();
				System.out.println("Closing db connection....");
				dbConnection = null;
			}
		}finally {
			lock.unlock();
		}
	}

	public String retrieveNickname(String username) throws SQLException {
		lock.lock();
		try{
			if(isConnectionValid()) {
				String query = "SELECT userNickname FROM users WHERE username = ?";
				String nickname = null;

				try (PreparedStatement preparedStatement = dbConnection.prepareStatement(query)) {
					preparedStatement.setString(1, username);
					try (ResultSet resultSet = preparedStatement.executeQuery()) {
						if (resultSet.next()) {
							nickname = resultSet.getString("userNickname");
						}
					}
				} catch (SQLException e) {
					System.err.println("Error while retrieving nickname: " + e.getMessage());
				}

				return nickname;

			}else{
				throw new SQLException("Database connection is not valid.");
			}



		}finally {
			lock.unlock();
		}

	}


	public boolean setUser(JSONObject user) throws SQLException {
		lock.lock();
		try{
			if(isConnectionValid()) {
				byte[] bytes = new byte[13];
				secureRandom.nextBytes(bytes);

				String saltBytes = new String(Base64.getEncoder().encode(bytes));
				String salt = "$6$" + saltBytes;
				String hashedPassword = Crypt.crypt(user.getString("password"), salt);

				if (userExists(user.getString("username"))) {
					return false;
				}
				String setUserString = "INSERT INTO users (username, password, email, userNickname) VALUES (?, ?, ?, ?)";

				try (PreparedStatement preparedStatement = dbConnection.prepareStatement(setUserString)) {
					preparedStatement.setString(1, user.getString("username"));
					preparedStatement.setString(2, hashedPassword);
					preparedStatement.setString(3, user.getString("email"));
					preparedStatement.setString(4, user.getString("userNickname"));
					preparedStatement.executeUpdate();
				}
				return true;
			}else{
				throw new SQLException("Database connection is not valid.");
			}
		}finally {
			lock.unlock();
		}
	}

	public boolean userExists(String user) throws SQLException {
		lock.lock();
		try{
			if(isConnectionValid()) {
				String checkUser = "SELECT username FROM users WHERE username = ?";
				try (PreparedStatement preparedStatement = dbConnection.prepareStatement(checkUser)) {
					preparedStatement.setString(1, user);
					try (ResultSet resultSet = preparedStatement.executeQuery()) {
						return resultSet.next();
					}
				}}
				else{
					throw new SQLException("Database connection is not valid.");
				}
		}finally {
			lock.unlock();
		}
	}

	public boolean authenticateUser(String username, String userPassword) throws SQLException {
		lock.lock();
		try{
			String query = "SELECT password FROM users WHERE username = ?";
			if(isConnectionValid()) {
				try (PreparedStatement preparedStatement = dbConnection.prepareStatement(query)) {
					preparedStatement.setString(1, username);
					try (ResultSet resultSet = preparedStatement.executeQuery()) {
						if (!resultSet.next()) {
							return false;
						}
						String hashedPassword = resultSet.getString("password");
						return hashedPassword.equals(Crypt.crypt(userPassword, hashedPassword));
					}
				}
			}
			else{
				throw new SQLException("Database connection is not valid.");
			}
		}finally {
			lock.unlock();
		}
	}





	public boolean insertObservationRecord(Message message) throws SQLException {
		if (message.getRecordIdentifier().isEmpty()) {
			throw new IllegalArgumentException("Invalid message format: recordIdentifier is empty.");
		}
		if (!isConnectionValid()) {
			throw new SQLException("Database connection is not valid.");
		}

		String insertObservatoryQuery = "INSERT INTO Observatory (observatoryName, latitude, longitude, "
			+ "temperatureInKelvins, cloudinessPercentance, bagroundLightVolume) "
			+ "VALUES (?, ?, ?, ?, ?, ?)";
		String insertMessageQuery = "INSERT INTO messages (recordIdentifier, recordDescription, recordPayload, "
			+ "recordRightAscension, recordDeclination, recordTimeReceived, modified, recordOwner, observatoryId, updatereason, messagePoster) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		lock.lock();
		try {
			dbConnection.setAutoCommit(false);

			Integer observatoryId = insertObservatory(dbConnection, message, insertObservatoryQuery);

			if (observatoryId == null) {
				throw new SQLException("Failed to retrieve generated ID for observatory.");
			}

			insertMessage(dbConnection, message, insertMessageQuery, observatoryId);

			dbConnection.commit();
			return true;
		} catch (SQLException e) {
			dbConnection.rollback();
			throw new SQLException("Failed to insert: " + e.getMessage(), e);
		} finally {
			dbConnection.setAutoCommit(true);
			lock.unlock();
		}
	}


	private Integer insertObservatory(Connection dbConnection, Message message, String insertObservatoryQuery) throws SQLException {
		try (PreparedStatement observatoryStatement = dbConnection.prepareStatement(insertObservatoryQuery, Statement.RETURN_GENERATED_KEYS)) {
			Observatory observatory = message.getObservatory();
			if (observatory == null) {
				observatoryStatement.setNull(1, Types.VARCHAR);
				observatoryStatement.setNull(2, Types.DOUBLE);
				observatoryStatement.setNull(3, Types.DOUBLE);
			} else {
				observatoryStatement.setString(1, observatory.getObservatoryName());
				observatoryStatement.setDouble(2, observatory.getLatitude());
				observatoryStatement.setDouble(3, observatory.getLongitude());
			}

			ObservatoryWeather weather = message.getObservatoryWeather();
			if (weather == null) {
				observatoryStatement.setNull(4, Types.DOUBLE);
				observatoryStatement.setNull(5, Types.DOUBLE);
				observatoryStatement.setNull(6, Types.DOUBLE);
			} else {
				observatoryStatement.setDouble(4, weather.getTemperatureInKelvins());
				observatoryStatement.setDouble(5, weather.getCloudinessPercentance());
				observatoryStatement.setDouble(6, weather.getBagroundLightVolume());
			}

			observatoryStatement.executeUpdate();

			try (ResultSet generatedKeys = observatoryStatement.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					return generatedKeys.getInt(1);
				} else {
					return null;
				}
			}
		}
	}

	private void insertMessage(Connection dbConnection, Message message, String insertMessageQuery, int observatoryId) throws SQLException {
		try (PreparedStatement messageStatement = dbConnection.prepareStatement(insertMessageQuery)) {
			messageStatement.setString(1, message.getRecordIdentifier());
			messageStatement.setString(2, message.getRecordDescription());
			messageStatement.setString(3, message.getRecordPayload());
			messageStatement.setString(4, message.getRecordRightAscension());
			messageStatement.setString(5, message.getRecordDeclination());
			messageStatement.setLong(6, message.dateAsInt());
			messageStatement.setNull(7, Types.BIGINT);
			messageStatement.setString(8, message.getRecordOwner());
			messageStatement.setInt(9, observatoryId);
			messageStatement.setString(10, message.getUpdatereason());
			messageStatement.setString(11, message.getMessagePoster());

			messageStatement.executeUpdate();
		}
	}

	public  JSONArray retrieveParamerterizedObservationRecords( Map<String, String> parameters) throws SQLException {
		StringBuilder queryBuilder = new StringBuilder(
			"SELECT m.*, o.observatoryName, o.latitude, o.longitude, " +
				"o.temperatureInKelvins, o.cloudinessPercentance, o.bagroundLightVolume " +
				"FROM messages m " +
				"LEFT JOIN observatory o ON m.observatoryId = o.id " +
				"WHERE 1=1"
		);
		JSONArray messages = new JSONArray();

		List<Object> parameterValues = new ArrayList<>();

		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();

			switch (key) {
				case "identification":
					queryBuilder.append(" AND m.recordIdentifier = ?");
					parameterValues.add(value);
					break;
				case "nickname":
					queryBuilder.append(" AND m.recordOwner = ?");
					parameterValues.add(value);
					break;
				case "before":
					queryBuilder.append(" AND m.recordTimeReceived <= ?");
					value = URLDecoder.decode(value, StandardCharsets.UTF_8);
					Instant instant = Instant.parse(value);
					parameterValues.add(instant.toEpochMilli());
					break;
				case "after":
					queryBuilder.append(" AND m.recordTimeReceived >= ?");
					value = URLDecoder.decode(value, StandardCharsets.UTF_8);
					Instant instant2 = Instant.parse(value);
					parameterValues.add(instant2.toEpochMilli());


					break;
				case "locations":
					JSONArray locationsArray = new JSONArray(value);
					if (locationsArray.length() > 0) {
						queryBuilder.append(" AND m.recordIdentifier IN (");
						for (int i = 0; i < locationsArray.length(); i++) {
							queryBuilder.append("?");
							if (i < locationsArray.length() - 1) {
								queryBuilder.append(",");
							}
							parameterValues.add(locationsArray.getString(i));
						}
						queryBuilder.append(")");
					}
					break;
				default:

					break;
			}
		}
		String query = queryBuilder.toString();
		System.out.println(query);
		lock.lock();
		try(PreparedStatement statement = dbConnection.prepareStatement(query)) {
			for (int i = 0; i < parameterValues.size(); i++) {
				statement.setObject(i + 1, parameterValues.get(i));

			}
			messages = Utils.extractMessage(statement);
			System.out.println("final extracted Message: " + messages);



		}finally {
			lock.unlock();
		}

		return messages;
	}

	public JSONArray retrieveObservationRecord() throws SQLException {
		if (!isConnectionValid()) {
			throw new SQLException("Database connection is not valid.");
		}

		JSONArray errorResponse = new JSONArray();
		String query = "SELECT m.id, m.recordIdentifier, m.recordDescription, m.recordPayload, "
			+ "m.recordRightAscension, m.recordDeclination, m.recordTimeReceived, m.modified,"
			+ "m.updatereason, m.messagePoster,"
			+ "m.recordOwner, o.observatoryName, o.latitude, o.longitude, "
			+ "o.temperatureInKelvins, o.cloudinessPercentance, o.bagroundLightVolume "
			+ "FROM messages m "
			+ "LEFT JOIN observatory o ON m.observatoryId = o.id";

		JSONArray messages = new JSONArray();
		lock.lock();
		try (PreparedStatement statement = dbConnection.prepareStatement(query)) {
			messages = Utils.extractMessage(statement);
		} finally {
			lock.unlock();
		}

		return messages;
	}
	protected int updateMessageData(int id, JSONObject jsonRequest, String username) throws SQLException {
		System.out.println("updateMessageData: " + jsonRequest);
		String recordDescription = jsonRequest.getString("recordDescription");
		String recordPayload = jsonRequest.getString("recordPayload");
		String recordRightAscension = jsonRequest.getString("recordRightAscension");
		String recordDeclination = jsonRequest.getString("recordDeclination");
		JSONArray  observatoryArray = jsonRequest.optJSONArray("observatory");

		String recordOwner = jsonRequest.getString("recordOwner");
		String updateReason = jsonRequest.optString("updateReason", "N/A");
		String time = jsonRequest.optString("recordTimeReceived");
		Instant instant = Instant.parse(time);
		long recordTimeReceived = instant.toEpochMilli();


		String checkMessageQuery = "SELECT id FROM messages WHERE id = ? AND messagePoster = ?";
		String updateMessageQuery = "UPDATE messages SET recordDescription = ?, recordPayload = ?, "
			+ "recordRightAscension = ?, recordDeclination = ?, recordTimeReceived = ?,"
			+ "modified = ?,"
			+ (recordOwner.isEmpty() ? "" : "recordOwner = ?, ")
		    + "updatereason = ? WHERE id = ?";


		String updateObservatoryQuery = "UPDATE observatory SET observatoryName = ?, latitude = ?, longitude = ? WHERE id = ?";

		lock.lock();
		try {
			try (PreparedStatement checkMessageStmt = dbConnection.prepareStatement(checkMessageQuery)) {
				checkMessageStmt.setInt(1, id);
				checkMessageStmt.setString(2, username);

				try (ResultSet resultSet = checkMessageStmt.executeQuery()) {
					if (!resultSet.next()) {
						System.out.println("No record found with id " + id + " or you are not authorized to update the message.");
						return 400;
					}
				}catch (SQLException e) {
					System.out.println("Error while checking record " + id + ": " + e.getMessage());
				}
			}catch (SQLException e) {
				System.out.println("Error while checking record " + id + ": " + e.getMessage());
			}

			try (PreparedStatement updateMessageStmt = dbConnection.prepareStatement(updateMessageQuery)) {
				updateMessageStmt.setString(1, recordDescription);
				updateMessageStmt.setString(2, recordPayload);
				updateMessageStmt.setString(3, recordRightAscension);
				updateMessageStmt.setString(4, recordDeclination);
				updateMessageStmt.setLong(5, recordTimeReceived);
				updateMessageStmt.setLong(6, Instant.now().toEpochMilli());

				//recordTimeReceived typo in assignments's payload example
				updateMessageStmt.setString(7, recordOwner);
				updateMessageStmt.setString(8, updateReason);
				updateMessageStmt.setInt(9, id);
				updateMessageStmt.executeUpdate();
			}catch (SQLException e) {
				System.out.println("Error while updating message for " + id + ": " + e.getMessage());
			}
            if(observatoryArray != null) {
				try (PreparedStatement updateObservatoryStmt = dbConnection.prepareStatement(updateObservatoryQuery)) {
					JSONObject observatory = jsonRequest.getJSONArray("observatory").getJSONObject(0);
					updateObservatoryStmt.setString(1, observatory.getString("observatoryName"));
					updateObservatoryStmt.setDouble(2, observatory.getDouble("latitude"));
					updateObservatoryStmt.setDouble(3, observatory.getDouble("longitude"));
					updateObservatoryStmt.setInt(4, id);
					updateObservatoryStmt.executeUpdate();
				}catch (SQLException e) {
					System.out.println("Error while updating observatory record " + id + ": " + e.getMessage());
				}
			}


			return 200;

		} catch (Exception e) {
			System.out.println("Error updating observation: " + e.getMessage());
			return 500;
		} finally {
			lock.unlock();
		}
	}

}







