package server;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.sql.Struct;

import com.sun.net.httpserver.BasicAuthenticator;
import java.util.concurrent.locks.ReentrantLock;


public class UserAuthenticator extends BasicAuthenticator {
	private MessageDB database;
	private  final ReentrantLock lock = new ReentrantLock();

	public UserAuthenticator() {
		super("datarecord");
		database = MessageDB.getInstance();
	}

	@Override
	public boolean checkCredentials(String username, String password) {
		boolean validator = false;
		lock.lock();
		try {
			validator = database.authenticateUser(username, password);
		} catch (SQLException e) {
			System.err.println("Error during user authentication: " + e.getMessage());
			return false;
		}finally {
			lock.unlock();
		}
		return validator;
	}

	public boolean addUser(String username, String password, String email, String userNickname) throws JSONException, SQLException {
		try {
			JSONObject userData = new JSONObject()
				.put("username", username)
				.put("password", password)
				.put("email", email)
				.put("userNickname", userNickname);

			boolean result = false;
			lock.lock();
			try{
				result = database.setUser(userData);

			}finally {
			      lock.unlock();
			}

			if (result) {
				System.out.println(username + " registered successfully!");
				return result;
			}
			System.out.println("Cannot register user: User may already exist.");
			return result;

		} catch (JSONException e) {
			System.err.println("Invalid JSON while adding user: " + e.getMessage());
			return false;
		} catch (SQLException e) {
			System.err.println("Database error while adding user: " + e.getMessage());
			return false;
		}
	}
}
