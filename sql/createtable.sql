CREATE TABLE `movies` (
   `id` int(11) NOT NULL AUTO_INCREMENT,
   `title` varchar(100) NOT NULL,
   `year` int(11) NOT NULL,
   `director` varchar(100) NOT NULL,
   `banner_url` varchar(200) DEFAULT NULL,
   `trailer_url` varchar(200) DEFAULT NULL,
   PRIMARY KEY (`id`),
   FULLTEXT(`title`)
 ) ENGINE=InnoDB AUTO_INCREMENT=907010 DEFAULT CHARSET=utf8;
CREATE TABLE `stars` (
   `id` int(11) NOT NULL AUTO_INCREMENT,
   `first_name` varchar(50) NOT NULL,
   `last_name` varchar(50) NOT NULL,
   `dob` date DEFAULT NULL,
   `photo_url` varchar(200) DEFAULT NULL,
   PRIMARY KEY (`id`)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE `stars_in_movies` (
   `star_id` int(11) NOT NULL,
   `movie_id` int(11) NOT NULL,
   KEY `id_idx` (`movie_id`),
   KEY `id_idx1` (`star_id`),
   CONSTRAINT `star_id` FOREIGN KEY (`star_id`) REFERENCES `stars` (`id`),
   CONSTRAINT `movie_id` FOREIGN KEY (`movie_id`) REFERENCES `movies` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE `genres` (
   `id` int(11) NOT NULL AUTO_INCREMENT,
   `name` varchar(32) NOT NULL,
   PRIMARY KEY (`id`)
 ) ENGINE=InnoDB AUTO_INCREMENT=907010 DEFAULT CHARSET=utf8;
CREATE TABLE `genres_in_movies` (
`genre_id` int(11) NOT NULL,
   `movie_id` int(11) NOT NULL,
   KEY `movie_id_idx` (`movie_id`),
   KEY `genre_id_idx` (`genre_id`),
   CONSTRAINT `genre_movie_id` FOREIGN KEY (`movie_id`) REFERENCES `movies` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
   CONSTRAINT `genre_id` FOREIGN KEY (`genre_id`) REFERENCES `genres` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE `creditcards` (
   `id` varchar(20) NOT NULL,
   `first_name` varchar(50) NOT NULL,
   `last_name` varchar(50) NOT NULL,
   `expiration` date NOT NULL,
   PRIMARY KEY (`id`)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE `customers` (
   `id` int(11) NOT NULL AUTO_INCREMENT,
   `first_name` varchar(50) NOT NULL,
   `last_name` varchar(50) NOT NULL,
   `cc_id` varchar(20) NOT NULL,
   `address` varchar(200) NOT NULL,
   `email` varchar(50) NOT NULL,
   `password` varchar(20) NOT NULL,
   PRIMARY KEY (`id`),
   KEY `cc_id_idx` (`cc_id`),
   CONSTRAINT `cc_id` FOREIGN KEY (`cc_id`) REFERENCES `creditcards` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
 ) ENGINE=InnoDB AUTO_INCREMENT=973021 DEFAULT CHARSET=utf8;
CREATE TABLE `sales` (
   `id` int(11) NOT NULL AUTO_INCREMENT,
   `customer_id` int(11) NOT NULL,
   `movie_id` int(11) NOT NULL,
   `sale_date` date NOT NULL,
   PRIMARY KEY (`id`),
   KEY `sales_customer_id_idx` (`customer_id`),
   KEY `sales_movie_id_idx` (`movie_id`),
   CONSTRAINT `sales_customer_id` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
   CONSTRAINT `sales_movie_id` FOREIGN KEY (`movie_id`) REFERENCES `movies` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
 ) ENGINE=InnoDB AUTO_INCREMENT=6452 DEFAULT CHARSET=utf8;
 CREATE TABLE `carts` (
   `session_id` varchar(50) NOT NULL,
   `movie_id` int(11) NOT NULL,
   `amount` int(11) NOT NULL,
   PRIMARY KEY (`session_id`,`movie_id`),
   KEY `carts_session_id_idx` (`session_id`),
   KEY `carts_movie_id_idx` (`movie_id`),
   CONSTRAINT `carts_movie_id` FOREIGN KEY (`movie_id`) REFERENCES `movies` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
 ) ENGINE=InnoDB AUTO_INCREMENT=6452 DEFAULT CHARSET=utf8