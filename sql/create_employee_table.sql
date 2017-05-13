DROP TABLE IF EXISTS `employees`;
CREATE TABLE IF NOT EXISTS `employees` (
	`email` varchar(50) primary key,
	`password` varchar(20) not null,
	`fullname` varchar(100)
 ) ENGINE=InnoDB AUTO_INCREMENT=907010 DEFAULT CHARSET=utf8;
 INSERT INTO employees VALUES ("classta@email.edu","classta","TA CS122B");