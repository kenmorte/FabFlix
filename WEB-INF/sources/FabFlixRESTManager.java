import java.io.StringWriter;
import java.sql.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FabFlixRESTManager
{
	private Connection mDatabase;
	private StringWriter mWriter;
	
	public static final String DATABASE_NAME = "moviedb";
	private static final String DATABASE_EMAIL = "mytestuser";
	private static final String DATABASE_PASSWORD = "mypassword";
	
	private static final String DATABASE_DEFAULT_ORDER_COLUMN = "title";
	private static final String DATABASE_DEFAULT_ORDER_TYPE = "asc";
	private static final String DATABASE_DEFAULT_OFFSET = "0";
	private static final String DATABASE_DEFAULT_LIMIT = "10";
	
	
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
				DATABASE_EMAIL, 
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
			writeErrorMessage("{\"error\": \"SQLException\", \"message\": \"" + e.getMessage() + "\"}");
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
			writeErrorMessage("{\"error\": \"SQLException\", \"message\": \"" + e.getMessage() + "\"}");
			return false;
		}
	}
	
	/**
	 * Retrieves a JSON object of a customer row given an email and password. 
	 * If the email and password combination does not work, then null is returned.
	 * 
	 * JSON Format:
	 * {
	 * 	"error": the name of the error raised trying to log in,
	 * 	"message": 'success' when the login was successful, otherwise the error message associated with an error,
	 * 	"user": {
	 * 				"id": user ID,
	 * 				"first_name": user's first name,
	 * 				"last_name": user's last name,
	 * 				"credit_card_id": user's credit card ID,
	 * 				"address": user's address,
	 * 				"email": user's email
	 * 			}
	 * }
	 * 
	 * @param email	customer's email
	 * @param password	customer's password
	 * @return	JSON object of the corresponding customer
	 * @throws SQLException		when there is an issue accessing/querying the database 
	 * @throws JSONException	when JSON is failed to parse
	 */
	public JSONObject getUserInfo(String email, String password) throws SQLException, JSONException {
		if (email == null || password == null) {
			writeErrorMessage("{\"error\": \"LoginException\", \"message\": \"No email and/or password provided!\"}");
			return null;
		}
		
		Statement select;
		ResultSet result;
		JSONObject json = null;
		
		select = mDatabase.createStatement();
		result = select.executeQuery("select * from customers where email=\"" + email + "\" and password=\"" + password + "\"");
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
	 * Retrieves a JSON object of movies given the first character of the title.
	 * If no movies were found, then the corresponding JSON array is an empty list.
	 * Otherwise, the list contains JSONObjects of resulting movies.
	 * 
	 * JSON Format:
	 * [
	 * 		{
	 * 			"movie_id": id of the first movie,
	 * 			"movie_title": title of the first movie (should be hyper-linked in HTML),
	 * 			"movie_year": year the first movie was published,
	 * 			"movie_director": director of the first movie,
	 * 			"movie_banner_url": URL of the first movie's banner,
	 * 			"movie_trailer_url": URL of the first movie's trailer,
	 * 			"movie_genres": [
	 * 								genre1,
	 * 								genre2,
	 * 								...
	 * 							],
	 * 			"movie_stars": [
	 * 								{
	 * 									"star_id": id of the first movie's first star,
	 * 									"star_first_name": first name of the first movie's first star,
	 * 									"star_last_name": last name of the first movie's first star
	 * 								},
	 * 								{
	 * 									"star_id": id of the first movie's second star,
	 * 									"star_first_name": first name of the first movie's second star,
	 * 									"star_last_name": last name of the first movie's second star
	 * 								},
	 * 								...
	 * 							]
	 * 		},
	 *		...
	 * ]
	 * 
	 * @param character	character that the movie titles start with
	 * @param orderColumn the column to order the resulting list by (should be either "title" or "year")
	 * @param orderType	either "desc" for descending or "asc" for ascending
	 * @return	JSON array containing the format above for movies starting with a specified character
	 */
	public JSONArray getMoviesByFirstCharacter(String character, 
		String orderColumn, String orderType, 
		String offset, String limit) throws SQLException, JSONException {
		if (character == null) {
			writeJSONErrorMessage("NullCharacterException", "No character provided for retrieving movies by title!");
			return null;
		}
		if (character.length() != 1) {
			writeJSONErrorMessage("CharacterLengthException", "Character provided for retrieving movies by title is not of length 1!");
			return null;
		}
		if (orderColumn == null)
			orderColumn = DATABASE_DEFAULT_ORDER_COLUMN;
		if (orderType == null)
			orderType = DATABASE_DEFAULT_ORDER_TYPE;
		if (offset == null)
			offset = DATABASE_DEFAULT_OFFSET;
		if (limit == null)
			limit = DATABASE_DEFAULT_LIMIT;
		
		
		Statement select;
		PreparedStatement genreStatement, starsStatement;
		ResultSet movieResult, genreResult, starsResult;
		JSONArray resultJSONArray, genresJSONArray, starsJSONArray;
		JSONObject movieJSON, starJSON;
		
		orderColumn = (orderColumn == null) ? "title" : orderColumn.equalsIgnoreCase("year") ? "year" : "title";
		orderType = (orderType == null || orderType.equalsIgnoreCase("desc")) ? "desc" : "asc";
		select = mDatabase.createStatement();
		// Replace ? with the movie ID
		genreStatement = mDatabase.prepareStatement("select g.name from genres g, genres_in_movies gm where gm.movie_id = ? and gm.genre_id = g.id;");
		starsStatement = mDatabase.prepareStatement("select s.id, s.first_name,s.last_name from stars s, stars_in_movies sm where sm.movie_id = ? and sm.star_id = s.id;");
		movieResult = select.executeQuery("select * from movies where title like \"" + character + "%\" order by " + orderColumn + " limit " + limit + " offset " + offset);
		resultJSONArray = new JSONArray();
		
		// Collect all the movies from the database
		while (movieResult.next()) {
			Integer movieID = movieResult.getInt(1);
			genresJSONArray = new JSONArray();
			starsJSONArray = new JSONArray();
			movieJSON = new JSONObject();
			
			// Collect all the genres associated with the current movie
			genreStatement.setInt(1, movieID);
			genreResult = genreStatement.executeQuery();
			while (genreResult.next()) {
				genresJSONArray.put(genreResult.getString(1));
			}
			
			// Collect all the stars associated with the current movie
			starsStatement.setInt(1, movieID);
			starsResult = starsStatement.executeQuery();
			while (starsResult.next()) {
				starJSON = new JSONObject();
				starJSON.put("star_id", starsResult.getInt(1));
				starJSON.put("star_first_name", starsResult.getString(2));
				starJSON.put("star_last_name", starsResult.getString(3));
				starsJSONArray.put(starJSON);
			}
			
			// Put all the elements inside our resulting movie JSON
			movieJSON.put("movie_id", movieID);
			movieJSON.put("movie_title", movieResult.getString(2));
			movieJSON.put("movie_year", movieResult.getInt(3));
			movieJSON.put("movie_director", movieResult.getString(4));
			movieJSON.put("movie_banner_url", movieResult.getString(5));
			movieJSON.put("movie_trailer_url", movieResult.getString(6));
			movieJSON.put("movie_genres", genresJSONArray);
			movieJSON.put("movie_stars", starsJSONArray);
			
			resultJSONArray.put(movieJSON);
		}
		return resultJSONArray;
	}
	
	/**
	 * Writes an error message to the writer for output to a web page.
	 * @param message
	 */
	private void writeErrorMessage(String message) {
		mWriter.flush();
		mWriter.write(message);
	}
	
	private void writeJSONErrorMessage(String error, String message) {
		writeErrorMessage("{\"error\":\"" + error + "\", \"message\": \"" + message + "\"}");
	}
}
