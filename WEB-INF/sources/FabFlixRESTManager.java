import java.io.StringWriter;
import java.sql.*;
import org.json.JSONException;
import org.json.JSONObject;

public class FabFlixRESTManager
{
	private Connection mDatabase;
	private StringWriter mWriter;
	public static final String DATABASE_NAME = "moviedb";
	private static final String DATABASE_email = "mytestuser";
	private static final String DATABASE_PASSWORD = "mypassword";
	
	public FabFlixRESTManager(StringWriter writer)  throws Exception {
		// Incorporate mySQL driver
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		mWriter = writer;
	}
	
	/**
	 * Attempts to make a connection with MySQL database with a specified user name and password.
	 * Uses the database login credentials (not the ones specified from the schema).
	 * 
	 * @return	<i>null</i> if connection was successful, a <i>SQLException</i> object if the connection failed
	 */
	public SQLException attemptConnection() {
		try {
			// Attempt to create a connection to the database
			mDatabase = DriverManager.getConnection(
				"jdbc:mysql:///" + FabFlixRESTManager.DATABASE_NAME + "?useSSL=false", 
				DATABASE_email, 
				DATABASE_PASSWORD);
			
		} catch (SQLException e) {
			mDatabase = null;
			return e;
		}
		return null;
	}
	
	/**
	 * Closes the database connection and releases any used resources from the connection.
	 */
	public void closeConnection() {
		try {
			if (mDatabase != null && !mDatabase.isClosed())
				mDatabase.close();
		} catch (SQLException e) {
			writeErrorMessage("{error: 'SQLException', user: null, message: '" + e.getMessage() + "'}");
		}
	}
	
	/**
	 * Returns the status of the connection to the database from the JDBC program.
	 * 
	 * @return	true if the connection is live, false otherwise
	 */
	public boolean isConnectionLive() {
		try {
			return mDatabase != null && !mDatabase.isClosed();
			
		} catch (SQLException e) {
			writeErrorMessage("{error: 'SQLException', user: null, message: '" + e.getMessage() + "'}");
			return false;
		}
	}
	
	public JSONObject getUserInfo(String email, String password) throws SQLException, JSONException {
		if (email == null || password == null) {
			writeErrorMessage("{error: 'LoginException', user: null, message: 'No email and/or password provided!'}");
			return null;
		}
		
		Statement select;
		ResultSet result;
		JSONObject json = null;
		
		select = mDatabase.createStatement();
		result = select.executeQuery("select * from customers where email='" + email + "' and password='" + password + "'");
		if (result.next()) {
			json = new JSONObject();
			json.put("id", result.getInt(1));
			json.put("first_name", result.getString(2));
			json.put("last_name", result.getString(3));
			json.put("credit_card_id", result.getString(4));
			json.put("address", result.getString(5));
			json.put("email", result.getString(6));
		}
		return json;
	}
	
	/**
	 * Writes an error message to the writer for output to a web page.
	 * @param message
	 */
	private void writeErrorMessage(String message) {
		mWriter.flush();
		mWriter.write(message);
	}
}
