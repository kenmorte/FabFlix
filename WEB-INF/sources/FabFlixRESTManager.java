import java.io.StringWriter;
import java.sql.*;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FabFlixRESTManager
{
	private Connection mDatabase;
	private StringWriter mWriter;
	
	private static final String DATABASE_NAME = "moviedb";
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
	 * {
	 * 	number_of_results: number of total results from the query (not affected by limit)
	 * 	movie_data:	[
	 * 		{
	 * 			"movie_id": id of the first movie,
	 * 			"movie_title": title of the first movie (should be hyper-linked in HTML),
	 * 			"movie_year": year the first movie was published,
	 * 			"movie_director": director of the first movie,
	 * 			"movie_banner_url": URL of the first movie's banner,
	 * 			"movie_trailer_url": URL of the first movie's trailer,
	 * 			"movie_cart_quantity": quantity of this movie inside a user session's cart, 
	 * 			"movie_genres": [
	 * 				genre1,
	 * 				genre2,
	 * 				...
	 * 			],
	 * 			"movie_stars": [
	 *					{
	 * 						"star_id": id of the first movie's first star,
	 * 						"star_first_name": first name of the first movie's first star,
	 * 						"star_last_name": last name of the first movie's first star
	 * 					},
	 * 					{
	 * 						"star_id": id of the first movie's second star,
	 * 						"star_first_name": first name of the first movie's second star,
	 * 						"star_last_name": last name of the first movie's second star
	 * 					},
	 * 					...
	 * 			]
	 * 		},
	 *		...
	 * 	]
	 * }
	 * 
	 * @param character	character that the movie titles start with
	 * @param orderColumn the column to order the resulting list by (should be either "title" or "year")
	 * @param orderType	either "desc" for descending or "asc" for ascending
	 * @return	JSON array containing the format above for movies starting with a specified character
	 * @throws SQLException if there was an error parsing the SQL command
	 * @throws JSONException if there was an error parsing the JSON object
	 */
	public JSONObject getMoviesByFirstCharacter(
		String sessionId,
		String character, 
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
		
		
		Statement select, nMoviesSelect;
		PreparedStatement genreStatement, starsStatement, cartsStatement;
		ResultSet movieResult, genreResult, starsResult, cartsResult, nMoviesResult;
		JSONArray movieDataJSONArray, genresJSONArray, starsJSONArray;
		JSONObject movieJSON, starJSON, resultJSON;
		
		// Use the default order column/type
		orderColumn = orderColumn.equalsIgnoreCase("year") ? "year" : DATABASE_DEFAULT_ORDER_COLUMN;
		orderType = orderType.equalsIgnoreCase("desc") ? "desc" : DATABASE_DEFAULT_ORDER_TYPE;
		
		select = mDatabase.createStatement();
		nMoviesSelect = mDatabase.createStatement();
		// Replace ? with the movie ID
		genreStatement = mDatabase.prepareStatement("select g.name from genres g, genres_in_movies gm where gm.movie_id = ? and gm.genre_id = g.id;");
		starsStatement = mDatabase.prepareStatement("select s.id, s.first_name,s.last_name from stars s, stars_in_movies sm where sm.movie_id = ? and sm.star_id = s.id;");
		cartsStatement = mDatabase.prepareStatement("select amount from carts where session_id = \"" + sessionId + "\" and movie_id = ?");
		movieResult = select.executeQuery("select * from movies where title like \"" + 
				character + "%\" order by " + 
				orderColumn + " " + 
				orderType + " limit " + 
				limit + " offset " + 
				offset);
		nMoviesResult = nMoviesSelect.executeQuery("select count(*) from movies where title like \"" + character +  "%\"");
		resultJSON = new JSONObject();
		movieDataJSONArray = new JSONArray();
		
		// Get the number of resulting movies from the database
		if (nMoviesResult.next())
			resultJSON.put("number_of_results", nMoviesResult.getInt(1));
		else
			resultJSON.put("number_of_results", 0);
		
		// Collect all the movies from the database
		while (movieResult.next()) {
			Integer movieID = movieResult.getInt(1);
			Integer cartAmount = 0;
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
			
			// Get the amount of this movie inside the cart for a specific user session
			cartsStatement.setInt(1, movieID);
			cartsResult = cartsStatement.executeQuery();
			if (cartsResult.next()) {
				cartAmount = cartsResult.getInt(1);
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
			movieJSON.put("movie_cart_quantity", cartAmount);
			
			movieDataJSONArray.put(movieJSON);
		}
		resultJSON.put("movie_data", movieDataJSONArray);
		return resultJSON;
	}
	
	/**
	 * Retrieves a JSON object of movies given the genre name for the query parameter.
	 * If no movies were found, then the corresponding JSON array is an empty list.
	 * Otherwise, the list contains JSONObjects of resulting movies.
	 * 
	 * To view the JSON format, view the doc-string for FabFlixRESTManager::getMoviesByFirstCharacter(...).
	 * 
	 * @param genre	genre name for the movies to be queried
	 * @param orderColumn the column to order the resulting list by (should be either "title" or "year")
	 * @param orderType either "desc" for descending or "asc" for ascending
	 * @param offset starting point from the resulting rows in the table
	 * @param limit number of results limited to display
	 * @return	JSON array containing the format above for movies with a given genre
	 * @throws SQLException if there was an error parsing the SQL command
	 * @throws JSONException if there was an error parsing the JSON object
	 */
	public JSONObject getMoviesByGenre(
		String sessionId,
		String genre,
		String orderColumn, String orderType, 
		String offset, String limit) throws SQLException, JSONException {
		if (genre == null)
			throw new SQLException("No genre specified! Unable to execute query");
		
		Statement select, nMoviesSelect;
		PreparedStatement genreStatement, starsStatement, cartsStatement;
		ResultSet movieResult, genreResult, starsResult, cartsResult, nMoviesResult;
		JSONArray movieDataJSONArray, genresJSONArray, starsJSONArray;
		JSONObject movieJSON, starJSON, resultJSON;
		
		select = mDatabase.createStatement();
		nMoviesSelect = mDatabase.createStatement();
		// Replace ? with the movie ID
		genreStatement = mDatabase.prepareStatement("select g.name from genres g, genres_in_movies gm where gm.movie_id = ? and gm.genre_id = g.id;");
		starsStatement = mDatabase.prepareStatement("select s.id, s.first_name,s.last_name from stars s, stars_in_movies sm where sm.movie_id = ? and sm.star_id = s.id;");
		cartsStatement = mDatabase.prepareStatement("select amount from carts where session_id = \"" + sessionId + "\" and movie_id = ?");
		movieResult = select.executeQuery("select m.* from genres g, movies m, genres_in_movies gm where g.name=\"" + 
				genre + "\" and gm.genre_id = g.id and gm.movie_id = m.id order by " + 
				orderColumn + " " + 
				orderType + " limit " + 
				limit + " offset " + 
				offset);
		nMoviesResult = nMoviesSelect.executeQuery("select count(*) from genres g, movies m, genres_in_movies gm where g.name=\"" + 
				genre + "\" and gm.genre_id = g.id and gm.movie_id = m.id");
		resultJSON = new JSONObject();
		movieDataJSONArray = new JSONArray();
		
		// Get the number of resulting movies from the database
		if (nMoviesResult.next())
			resultJSON.put("number_of_results", nMoviesResult.getInt(1));
		else
			resultJSON.put("number_of_results", 0);
		
		// Collect all the movies from the database
		while (movieResult.next()) {
			Integer movieID = movieResult.getInt(1);
			Integer cartAmount = 0;
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
			
			// Get the amount of this movie inside the cart for a specific user session
			cartsStatement.setInt(1, movieID);
			cartsResult = cartsStatement.executeQuery();
			if (cartsResult.next()) {
				cartAmount = cartsResult.getInt(1);
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
			movieJSON.put("movie_cart_quantity", cartAmount);
			
			movieDataJSONArray.put(movieJSON);
		}
		resultJSON.put("movie_data", movieDataJSONArray);
		return resultJSON;
	}
	
	/**
	 * Retrieves a JSON object of movies given various query parameters.
	 * If no movies were found, then the corresponding JSON array is an empty list.
	 * Otherwise, the list contains JSONObjects of resulting movies.
	 * 
	 * @param title	title parameter for a movie
	 * @param year	year parameter for when movie is released
	 * @param director	director parameter for movie
	 * @param starFirstName	first name parameter for a star in a movie
	 * @param starLastName	last name parameter for a star in a movie
	 * @param orderColumn the column to order the resulting list by (should be either "title" or "year")
	 * @param orderType either "desc" for descending or "asc" for ascending
	 * @param offset starting point from the resulting rows in the table
	 * @param limit number of results limited to display
	 * @return	JSON array containing the format above for movies with given parameters
	 * @throws SQLException if there was an error parsing the SQL command
	 * @throws JSONException if there was an error parsing the JSON object
	 */
	public JSONObject getMoviesByParameters(
		String sessionId,
		String title, 
		String year, 
		String director, 
		String starFirstName, String starLastName, 
		String orderColumn, String orderType, 
		String offset, String limit) throws SQLException, JSONException {
		
		if (orderColumn == null)
			orderColumn = DATABASE_DEFAULT_ORDER_COLUMN;
		if (orderType == null)
			orderType = DATABASE_DEFAULT_ORDER_TYPE;
		if (offset == null)
			offset = DATABASE_DEFAULT_OFFSET;
		if (limit == null)
			limit = DATABASE_DEFAULT_LIMIT;
		
		Statement select, nMoviesSelect;
		PreparedStatement genreStatement, starsStatement, cartsStatement;
		ResultSet movieResult, genreResult, starsResult, cartsResult, nMoviesResult;
		JSONArray movieDataJSONArray, genresJSONArray, starsJSONArray;
		JSONObject movieJSON, starJSON, resultJSON;
		
		// Use the default order column/type
		orderColumn = orderColumn.equalsIgnoreCase("year") ? "year" : DATABASE_DEFAULT_ORDER_COLUMN;
		orderType = orderType.equalsIgnoreCase("desc") ? "desc" : DATABASE_DEFAULT_ORDER_TYPE;
		
		select = mDatabase.createStatement();
		nMoviesSelect = mDatabase.createStatement();
		// Replace ? with the movie ID
		genreStatement = mDatabase.prepareStatement("select g.name from genres g, genres_in_movies gm where gm.movie_id = ? and gm.genre_id = g.id;");
		starsStatement = mDatabase.prepareStatement("select s.id, s.first_name,s.last_name from stars s, stars_in_movies sm where sm.movie_id = ? and sm.star_id = s.id;");
		cartsStatement = mDatabase.prepareStatement("select amount from carts where session_id = \"" + sessionId + "\" and movie_id = ?");
		movieResult = select.executeQuery(
			"select distinct m.* from stars s, stars_in_movies sm, movies m " 
			+ getSubstringMatchingWhereClause(title, year, director, starFirstName, starLastName)
			+ " order by " + orderColumn 
			+ " " + orderType 
			+ " limit " + limit 
			+ " offset " + offset
		);
		nMoviesResult = nMoviesSelect.executeQuery("select distinct m.* from stars s, stars_in_movies sm, movies m " + getSubstringMatchingWhereClause(title, year, director, starFirstName, starLastName));
		resultJSON = new JSONObject();
		movieDataJSONArray = new JSONArray();
		
		// Get the number of resulting movies from the database
		nMoviesResult.last();
		resultJSON.put("number_of_results", nMoviesResult.getRow());
		
		// Collect all the movies from the database
		while (movieResult.next()) {
			Integer movieID = movieResult.getInt(1);
			Integer cartAmount = 0;
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
			
			// Get the amount of this movie inside the cart for a specific user session
			cartsStatement.setInt(1, movieID);
			cartsResult = cartsStatement.executeQuery();
			if (cartsResult.next()) {
				cartAmount = cartsResult.getInt(1);
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
			movieJSON.put("movie_cart_quantity", cartAmount);
			
			movieDataJSONArray.put(movieJSON);
		}
		resultJSON.put("movie_data", movieDataJSONArray);
		return resultJSON;
	}
	
	/**
	 * Retrieves a JSON object for a movie given the ID of the movie trying to be queried.
	 * The movie JSON format follows:
	 * 
	 * 	{
	 * 		"movie_id": id of the movie,
	 * 		"movie_title": title of the movie,
	 * 		"movie_year": year the movie was published,
	 * 		"movie_director": director of the movie,
	 * 		"movie_banner_url": URL of the movie's banner,
	 * 		"movie_trailer_url": URL of the movie's trailer,
	 * 		"movie_cart_quantity": quantity of this movie inside a user session's cart,
	 * 		"movie_genres": [
	 * 			genre1,
	 * 			genre2,
	 * 			...
	 * 		],
	 * 		"movie_stars": [
	 *				{
	 * 					"star_id": id of the movie's first star,
	 * 					"star_first_name": first name of the movie's first star,
	 * 					"star_last_name": last name of the movie's first star
	 * 				},
	 * 				{
	 * 					"star_id": id of the movie's second star,
	 * 					"star_first_name": first name of the movie's second star,
	 * 					"star_last_name": last name of the movie's second star
	 * 				},
	 * 				...
	 * 			]
	 * 	}
	 * 
	 * @param id ID of the queried movie
	 * @return	JSON object representing the movie queried, or null if none was found
	 * @throws SQLException if there was an error in the SQL command or the ID was null
	 * @throws JSONException if there was an error parsing the resulting JSON object
	 */
	public JSONObject getMovieById(String sessionId, Integer id) throws SQLException, JSONException {
		if (id == null)
			throw new SQLException("No movie ID provided!");
		if (sessionId == null)
			throw new SQLException("No session ID provided for querying for one movie!");
		
		Statement select;
		PreparedStatement genreStatement, starsStatement, cartsStatement;
		ResultSet result, genreResult, starsResult, cartsResult;
		JSONArray genresJSONArray, starsJSONArray;
		JSONObject json = null, starJSON;
		
		select = mDatabase.createStatement();
		result = select.executeQuery("select * from movies where id = " + id);
		// Replace ? with the movie ID
		genreStatement = mDatabase.prepareStatement("select g.name from genres g, genres_in_movies gm where gm.movie_id = ? and gm.genre_id = g.id;");
		starsStatement = mDatabase.prepareStatement("select s.id, s.first_name,s.last_name from stars s, stars_in_movies sm where sm.movie_id = ? and sm.star_id = s.id;");
		cartsStatement = mDatabase.prepareStatement("select amount from carts where session_id = \"" + sessionId + "\" and movie_id = ?");
		
		if (result.next()) {
			Integer movieID = result.getInt(1);
			Integer cartAmount = 0;
			json = new JSONObject();
			genresJSONArray = new JSONArray();
			starsJSONArray = new JSONArray();
			
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
			
			// Get the amount of this movie inside the cart for a specific user session
			cartsStatement.setInt(1, movieID);
			cartsResult = cartsStatement.executeQuery();
			if (cartsResult.next()) {
				cartAmount = cartsResult.getInt(1);
			}
			
			json.put("movie_id", movieID);
			json.put("movie_title", result.getString(2));
			json.put("movie_year", result.getInt(3));
			json.put("movie_director", result.getString(4));
			json.put("movie_banner_url", result.getString(5));
			json.put("movie_trailer_url", result.getString(6));
			json.put("movie_genres", genresJSONArray);
			json.put("movie_stars", starsJSONArray);
			json.put("movie_cart_quantity", cartAmount);
		}
		
		return json;
	}
	
	/**
	 * Retrieves a JSON object for a star given the ID of the star trying to be queried.
	 * The star JSON format follows:
	 * 
	 * 	{
	 * 		"star_id": id of the star,
	 * 		"star_first_name": first name of the star,
	 * 		"star_last_name": last name of the star,
	 * 		"star_dob": star's date of birth,
	 * 		"star_photo_url": URL of the star's picture,
	 * 		"star_movies": [
	 *				{
	 * 					"movie_id": id of the first movie that features the star,
	 * 					"movie_title": title of the first movie that features the star,
	 * 					"movie_year": year the first movie released that features the star
	 * 				},
	 * 				{
	 * 					"movie_id": id of the second movie that features the star,
	 * 					"movie_title": title of the second movie that features the star,
	 * 					"movie_year": year the second movie released that features the star
	 * 				},
	 * 				...
	 * 			]
	 * 	}
	 * 
	 * @param id	ID of the queried star
	 * @return	JSON object representing the star queried, or null if none was found
	 * @throws SQLException	if there was an error in the SQL command or the ID was null
	 * @throws JSONException	if there was an error parsing the resulting JSON object
	 */
	public JSONObject getStarById(Integer id) throws SQLException, JSONException {
		if (id == null)
			throw new SQLException("No star ID provided!");
		
		Statement starSelect, movieSelect;
		ResultSet starResult, movieResult;
		JSONArray movieJSONArray;
		JSONObject starJSON = null, movieJSON;
		
		starSelect = mDatabase.createStatement();
		movieSelect = mDatabase.createStatement();
		starResult = starSelect.executeQuery("select * from stars where id = " + id);
		movieResult = movieSelect.executeQuery("select m.id, m.title, m.year from stars_in_movies sm, movies m where sm.star_id = " + id + " and sm.movie_id = m.id");
		
		if (starResult.next()) {
			starJSON = new JSONObject();
			movieJSONArray = new JSONArray();
			
			// Put the star attributes from star table
			starJSON.put("star_id", starResult.getInt(1));
			starJSON.put("star_first_name", starResult.getString(2));
			starJSON.put("star_last_name", starResult.getString(3));
			starJSON.put("star_dob", starResult.getDate(4));
			starJSON.put("star_photo_url", starResult.getString(5));
			
			// Get movies featuring the star
			while (movieResult.next()) {
				movieJSON = new JSONObject();
				movieJSON.put("movie_id", movieResult.getInt(1));
				movieJSON.put("movie_title", movieResult.getString(2));
				movieJSON.put("movie_year", movieResult.getInt(3));
				movieJSONArray.put(movieJSON);
			}
			starJSON.put("star_movies", movieJSONArray);
		}
		return starJSON;
	}
	
	/**
	 * Updates the cart for a user session for a given movie. Depending on the amount
	 * specified, the action taken will either be:
	 * 		0: Delete the movie
	 * 		> 1: Update/insert the movie, depending on if it was already present on the cart
	 * 
	 * @param sessionId	ID for a user session
	 * @param movieId	movie ID
	 * @param amount	amount to be updated on the cart
	 * @return JSON object w/number of rows affected from the operation (should return 1)
	 * @throws SQLException	if there was an error in the SQL command or the ID was null
	 * @throws JSONException	if there was an error parsing the resulting JSON object
	 */
	public JSONObject updateMovieInCart(String sessionId, Integer movieId, Integer amount) throws SQLException, JSONException {
		if (sessionId == null)
			throw new SQLException("No session ID provided for adding to cart!");
		if (movieId == null)
			throw new SQLException("No movie ID provided for adding to cart!");
		if (amount == null)
			throw new SQLException("No amount provided for adding to cart!");
		if (amount < 0)
			throw new SQLException("Unable to add negative amount to cart!");
		
		Statement statement;
		ResultSet result;
		Integer success;
		JSONObject data;
		
		statement = mDatabase.createStatement();
		result = statement.executeQuery("select * from carts " + 
				" where session_id = \"" + sessionId + "\" and" + 
				" movie_id = " + movieId
		);
		success = 0;
		data = new JSONObject();
		
		if (result.next()) {	// should only update row (movie is inside cart already
			
			// Update the value only if the amount if > 0
			if (amount > 0)
				success = statement.executeUpdate("update carts " + 
						"set amount = " + amount + 
						" where session_id = \"" + sessionId + "\"" + 
						" and movie_id = " + movieId
				);
			else 
				// Delete the movie from the cart if the amount is specified as 0
				success = statement.executeUpdate("delete from carts " + 
						" where session_id = \"" + sessionId + "\"" + 
						" and movie_id = " + movieId
				);
		} else
			// Insert the movie into the cart with a specified amount
			success = statement.executeUpdate("insert into carts" + 
					" values(\"" + sessionId + "\"," + movieId + "," + amount + ")"
			);
		
		data.put("success", success);
		return data;
	}
	
	/**
	 * Returns a JSON containing movie objects that are inside a specific
	 * user session's cart. This is inside the attribute "cartData". 
	 * If no movies were found, the array is empty.
	 * @see FabFlixRESTManager#getMovieById(String sessionId, Integer id) for movie JSON format
	 * 
	 * @param sessionId	ID for a user session
	 * @return	JSONArray containing movie JSON objects for the user's cart
	 * @throws SQLException	if there was an error in the SQL command or the ID was null
	 * @throws JSONException	if there was an error parsing the resulting JSON object
	 */
	public JSONObject getCartData(String sessionId) throws SQLException, JSONException {
		if (sessionId == null)
			throw new SQLException("No session ID provided for viewing cart data!");
		
		Statement cartStatement;
		ResultSet cartResult;
		JSONObject movieJSON, resultJSON;
		JSONArray result;
		
		cartStatement = mDatabase.createStatement();
		cartResult = cartStatement.executeQuery(
			"select m.*, c.amount" + 
			" from movies m, carts c" + 
			" where c.session_id = \"" + sessionId + "\" and c.movie_id = m.id"
		);
		result = new JSONArray();
		resultJSON = new JSONObject();
		
		while (cartResult.next()) {
			movieJSON = new JSONObject();
			movieJSON.put("movie_id", cartResult.getInt(1));
			movieJSON.put("movie_title", cartResult.getString(2));
			movieJSON.put("movie_year", cartResult.getInt(3));
			movieJSON.put("movie_director", cartResult.getString(4));
			movieJSON.put("movie_banner_url", cartResult.getString(5));
			movieJSON.put("movie_trailer_url", cartResult.getString(6));
			movieJSON.put("movie_cart_quantity", cartResult.getInt(7));
			result.put(movieJSON);
		}
		resultJSON.put("cartData", result);
		return resultJSON;
	}
	
	/**
	 * Returns a JSON response from validating credit card information provided from user.
	 * The format for the response is as follows:
	 * {
	 * 		"success": true or false, depending if the information is correct
	 * 		"message": message for the response, following more information after the attempt
	 * }
	 * 
	 * @param firstName	first name for the credit card information
	 * @param lastName	last name for the credit card information
	 * @param creditCardNumber	credit card ID from the database
	 * @param creditCardExpiration	expiration date for the credit card
	 * @return	JSON response of whether the information provided was correct or not
	 * @throws SQLException	if there was an error in the SQL command or the ID was null
	 * @throws JSONException	if there was an error parsing the resulting JSON object
	 */
	public JSONObject validateCreditCardInfo(
		String firstName, String lastName,
		String creditCardNumber,
		String creditCardExpiration) throws SQLException, JSONException {
		
		if (firstName == null)
			throw new SQLException("No first name provided for validating credit card information!");
		if (lastName == null)
			throw new SQLException("No last name provided for validating credit card information!");
		if (creditCardNumber == null)
			throw new SQLException("No credit card number provided for validating credit card information!");
		if (creditCardExpiration == null)
			throw new SQLException("No credit card expiration provided for validating credit card information!");
		
		Statement statement;
		ResultSet result;
		JSONObject json;
		
		statement = mDatabase.createStatement();
		result = statement.executeQuery("select * from creditcards" +
				" where id = \"" + creditCardNumber + "\" and" +
				" first_name = \"" + firstName + "\" and" +
				" last_name = \"" + lastName + "\" and" +
				" expiration = \"" + creditCardExpiration + "\""
		);
		json = new JSONObject();
		
		if (result.next()) {
			json.put("success", true);
			json.put("message", "Successfully validated credit card information!");
		}
		else {
			json.put("success", false);
			json.put("message", "Credit card information did not match any information in database. Please try again.");
		}
		
		return json;
	}
	
	/**
	 * Returns a JSON response from completing transaction for a movie from a user.
	 * The format for the response is as follows:
	 * {
	 * 		"success": true or false, depending if transaction was successful or not
	 * 		"message": message for the response, following more information after the transaction
	 * }
	 * 
	 * @param customerId	customer's ID for the transaction
	 * @param movieId	movie ID for the movie being sold
	 * @param transactionDate	date transaction took place
	 * @return	JSON response of whether the transaction was successful and message
	 * @throws SQLException	if there was an error in the SQL command or the ID was null
	 * @throws JSONException	if there was an error parsing the resulting JSON object
	 */
	public JSONObject submitMovieSale(
		String sessionId,
		String customerId, 
		String movieId, 
		String transactionDate) throws SQLException, JSONException {
		
		if (customerId == null)
			throw new SQLException("No customer ID provided for submitting movie sale!");
		if (movieId == null)
			throw new SQLException("No movie ID provided for submitting movie sale!");
		if (transactionDate == null)
			throw new SQLException("No transaction date provided for submitting movie sale!");
		
		Statement statement;
		Integer success;
		JSONObject json;
		statement = mDatabase.createStatement();
		System.out.println("insert into sales values(DEFAULT," + 
				" \"" + customerId + "\"," + 
				" \"" + movieId + "\"," + 
				" \"" + transactionDate + "\")");
		success = statement.executeUpdate("insert into sales values(DEFAULT," + 
				" \"" + customerId + "\"," + 
				" \"" + movieId + "\"," + 
				" \"" + transactionDate + "\")"
		);
		json = new JSONObject();
		
		if (success <= 0) {
			json.put("success", false);
			json.put("message", "Unable to complete transaction for movie with ID (" + movieId + ")!");
			
		} else {
			json.put("success", true);
			json.put("message", "Successfully completed transaction for movie with ID (" + movieId + ")!");
			
			// Delete the movie from the cart, it has completed its transaction
			statement.executeUpdate("delete from carts where session_id = \"" + sessionId + "\" and movie_id = \"" + movieId + "\"");
		}
		System.out.println(json.toString());
		return json;
	}
	
	/**
	 * Gets the where clause for substring matching given the following parameters.
	 * If all the parameters are null, then the output is an empty string.
	 * 
	 * @param title	title parameter for a movie
	 * @param year	year parameter for when movie is released
	 * @param director	director parameter for movie
	 * @param starFirstName	first name parameter for a star in a movie
	 * @param starLastName	last name parameter for a star in a movie
	 * @return	where clause for substring matching in a movie query
	 */
	private String getSubstringMatchingWhereClause(String title, String year, String director, 
		String starFirstName, String starLastName) {
		HashMap<String,String> map = new HashMap<>();
		String result = " where s.id = sm.star_id and m.id = sm.movie_id ";
		
		if (title != null)
			map.put("m.title", title);
		if (year != null)
			map.put("m.year", year);
		if (director != null)
			map.put("m.director", director);
		if (starFirstName != null)
			map.put("s.first_name", starFirstName);
		if (starLastName != null)
			map.put("s.last_name", starLastName);
		
		if (!map.keySet().isEmpty()) {
			for (String column : map.keySet()) {
				result += " and " + column + " like \"%" + map.get(column) + "%\"";
			}
		}
		return result;
	}
	
	/**
	 * Writes an error message to the writer for output to a web page.
	 * @param message
	 */
	private void writeErrorMessage(String message) {
		mWriter.flush();
		mWriter.write(message);
	}
	
	/**
	 * Writes an error message in JSON format.
	 * 
	 * @param error	name of the error occurred
	 * @param message	message for the corresponding error
	 */
	public void writeJSONErrorMessage(String error, String message) {
		writeErrorMessage("{\"error\":\"" + error + "\", \"message\": \"" + message + "\"}");
	}
}
