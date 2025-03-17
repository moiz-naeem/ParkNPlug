package com.o3.server;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.security.SecureRandom;
import java.util.Base64;

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
					+ "userNickname VARCHAR(50) NOT NULL UNIQUE"
					+ ")";

				String createObservatoryTable = "CREATE TABLE IF NOT EXISTS Observatory ("
					+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "observatoryName VARCHAR(100) NULL,"
					+ "latitude DOUBLE NULL,"
					+ "longitude DOUBLE NULL,"
					+ "temperatureInKelvins DOUBLE NULL,"
					+ "cloudinessPercentance DOUBLE NULL,"
					+ "backgroundLightVolume DOUBLE NULL"
					+ ");";

				// Create Message Table
				String createMessagesTable = "CREATE TABLE IF NOT EXISTS Message ("
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


	private boolean checkInvalidMessage(Message message) {
		if (message.getRecordIdentifier() == null || message.getRecordIdentifier().isEmpty()) {
			System.out.println("Invalid message: " + message);
			return true;
		}
		return false;

	}


	public boolean insertObservationRecord(Message message) throws SQLException {
		if(checkInvalidMessage(message)){

			throw new IllegalArgumentException("Invalid message format.");
		};
		if(!isConnectionValid()) {
			throw new SQLException("Database connection is not valid.");
		}

		System.out.println("Inserting data: ");
		String insertQuery = "INSERT INTO messages (recordIdentifier, recordDescription, recordPayload, "
			+ "recordRightAscension, recordDeclination, recordTimeReceived, recordOwner, observatoryName, latitude, longitude) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ? , ? , ?,?)";
		lock.lock();
		try (PreparedStatement preparedStatement = dbConnection.prepareStatement(insertQuery)) {

			preparedStatement.setString(1, message.getRecordIdentifier());
			preparedStatement.setString(2, message.getRecordDescription());
			preparedStatement.setString(3, message.getRecordPayload());
			preparedStatement.setString(4, message.getRecordRightAscension());
			preparedStatement.setString(5, message.getRecordDeclination());
			preparedStatement.setLong(6, message.dateAsInt());
			preparedStatement.setString(7, message.getRecordOwner());

			if (message.getObservatory() != null) {
				preparedStatement.setString(8, message.getObservatory().getObservatoryName());
				preparedStatement.setDouble(9, message.getObservatory().getLatitude());
				preparedStatement.setDouble(10, message.getObservatory().getLongitude());
			} else {
				preparedStatement.setNull(8, java.sql.Types.VARCHAR);
				preparedStatement.setNull(9, java.sql.Types.DOUBLE);
				preparedStatement.setNull(10, java.sql.Types.DOUBLE);
			}


			preparedStatement.executeUpdate();
			return true;
		}finally {
			lock.unlock();
		}
	}

	public JSONArray retrieveObservationRecord() throws SQLException {
		if(!isConnectionValid()){
			throw new SQLException("Database connection is not valid.");
		}
		JSONArray errorResponse = new JSONArray();
		// System.out.println("Querying recordIdentifier: " + recordIdentifier);

		// String query = "SELECT recordIdentifier, recordDescription, recordPayload, "
		//     + "recordRightAscension, recordDeclination, recordTimeReceived "
		//     + "FROM messages WHERE recordIdentifier = ?";

		String query = "SELECT recordIdentifier, recordDescription, recordPayload, "
			+ "recordRightAscension, recordDeclination, recordTimeReceived, recordOwner, observatoryName, latitude, longitude "
			+ "FROM messages";

		JSONArray messages = new JSONArray();
		lock.lock();
		try (PreparedStatement preparedStatement = dbConnection.prepareStatement(query)) {
			// preparedStatement.setString(1, recordIdentifier.trim());
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					try {
						String observatoryName = resultSet.getString("observatoryName");
						Double latitude = resultSet.getObject("latitude") != null ? resultSet.getDouble("latitude") : null;
						Double longitude = resultSet.getObject("longitude") != null ? resultSet.getDouble("longitude") : null;

						Observatory observatory = null;
						if(observatoryName != null && !observatoryName.isEmpty()) {
							observatory = new Observatory(observatoryName, latitude, longitude);
						}


						Message message = new Message(
							resultSet.getString("recordIdentifier"),
							resultSet.getString("recordDescription"),
							resultSet.getString("recordPayload"),
							resultSet.getString("recordRightAscension"),
							resultSet.getString("recordDeclination"),
							resultSet.getLong("recordTimeReceived"),
							resultSet.getString("recordOwner"),
							observatory

						);
						JSONObject json = new JSONObject();
						json.put("recordIdentifier", message.getRecordIdentifier());
						json.put("recordDescription", message.getRecordDescription());
						json.put("recordPayload", message.getRecordPayload());
						json.put("recordRightAscension", message.getRecordRightAscension());
						json.put("recordDeclination", message.getRecordDeclination());
						json.put("recordTimeReceived", message.getRecordTimeReceived());
						json.put("recordOwner", message.getRecordOwner());

						if(observatory != null) {
							JSONArray observation = new JSONArray();
							JSONObject observationRecord = new JSONObject();
							observationRecord.put("observatoryName", observatory.getObservatoryName());
							observationRecord.put("latitude", observatory.getLatitude());
							observationRecord.put("longitude", observatory.getLongitude());
							observation.put(observationRecord);
							json.put("observatory", observation);
						}

						messages.put(json);

					}catch (JSONException e) {
						System.err.println("JSOn error: " + e.getMessage());
						return errorResponse;

					}
				}
			}
		} catch (SQLException e) {
			System.err.println("SQL error: " + e.getMessage());
			return errorResponse;
		} catch (Exception e) {
			System.err.println("Unexpected error: " + e.getMessage());
			return errorResponse;
		}finally {
			lock.unlock();
		}

		System.out.println("Final JSON Response: " + messages);
		return new JSONArray(messages.toString());

	}



}
