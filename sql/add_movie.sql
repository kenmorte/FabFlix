-- Change DELIMITER to $$
DELIMITER $$

DROP PROCEDURE IF EXISTS `add_movie`;
CREATE PROCEDURE `add_movie` (
	IN title varchar(100), 
	IN year int(11), 
	IN director varchar(100),
	IN star_first_name varchar(50),
	IN star_last_name varchar(50),
	IN genre_name varchar(32)
)
BEGIN
	SET @success = false;
	SET @error = "";
	SET @n = 0;
	SET @movieCreated = false;
	SET @starCreated = false;
	SET @genreCreated = false;
	SET @starInserted = false;
	SET @genreInserted = false;

	IF ( SELECT NOT EXISTS (SELECT 1 FROM movies m WHERE m.title = title) )  THEN
		INSERT INTO movies VALUES (DEFAULT, title, year, director, DEFAULT, DEFAULT);
		SET @movieCreated = true;
	END IF;
	
	IF ( SELECT NOT EXISTS (	-- star doesn't exist in stars table, create one
		SELECT 1 FROM stars WHERE first_name = star_first_name AND last_name = star_last_name) ) THEN
		INSERT INTO stars VALUES (DEFAULT, star_first_name, star_last_name, DEFAULT, DEFAULT);
		SET @starCreated = true;
	END IF;
	
	IF ( SELECT NOT EXISTS (	-- genre doesn't exist in genre table, create one
		SELECT 1 FROM genres WHERE name = genre_name) ) THEN
		INSERT INTO genres VALUES (DEFAULT, genre_name);
		SET @genreCreated = true;
	END IF;
	
	-- Get corresponding movie, star, and genre id
	SET @movieID = (SELECT id FROM movies m WHERE m.title = title);
	SET @starID = (SELECT id FROM stars WHERE first_name = star_first_name AND last_name = star_last_name);
	SET @genreID = (SELECT id FROM genres WHERE name = genre_name);
	
	IF ( SELECT NOT EXISTS (	-- star doesn't exist in stars_in_movies table, create one
		SELECT 1 FROM stars_in_movies WHERE star_id = @starID AND movie_id = @movieID) ) THEN
		INSERT INTO stars_in_movies VALUES (@starID, @movieID);
		SET @starInserted = true;
	END IF;
	
	IF ( SELECT NOT EXISTS (	-- genre doesn't exist in genres_in_movies table, create one
		SELECT 1 FROM genres_in_movies WHERE genre_id = @genreID AND movie_id = @movieID) ) THEN
		INSERT INTO genres_in_movies VALUES (@genreID, @movieID);
		SET @genreInserted = true;
	END IF;
	
	SET @success = @starInserted OR @genreInserted OR @movieCreated OR @starCreated OR @genreCreated;
	SELECT  @success AS success, 
			@movieCreated AS movieCreated, 
			@starCreated AS starCreated, 
			@genreCreated AS genreCreated,
			@starInserted AS starInserted,
			@genreInserted AS genreInserted;
END 
$$