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
import java.util.ArrayList;
import java.util.Base64;

import org.apache.commons.codec.digest.Crypt;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
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
					+ ");";

				String createMessagesTable = "CREATE TABLE IF NOT EXISTS messages ("
					+ "recordIdentifier VARCHAR(50) NOT NULL PRIMARY KEY,"
					+ "recordDescription TEXT NOT NULL,"
					+ "recordPayload TEXT NOT NULL,"
					+ "recordRightAscension VARCHAR(50) NOT NULL,"
					+ "recordDeclination VARCHAR(50) NOT NULL,"
					+ "recordTimeReceived BIGINT NOT NULL,"
					+ "recordOwner VARCHAR(50),"
					+ "observatoryId INTEGER NULL,"
					+ "FOREIGN KEY (observatoryId) REFERENCES Observatory(id)"
					+ ");";


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
			+ "recordRightAscension, recordDeclination, recordTimeReceived, recordOwner, observatoryId) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

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
			messageStatement.setString(7, message.getRecordOwner());
			messageStatement.setInt(8, observatoryId);

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
		String query = "SELECT m.recordIdentifier, m.recordDescription, m.recordPayload, "
			+ "m.recordRightAscension, m.recordDeclination, m.recordTimeReceived, "
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




}
