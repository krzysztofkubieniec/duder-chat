CREATE DATABASE IF NOT EXISTS test;

ALTER DATABASE test
  DEFAULT CHARACTER SET utf8
  DEFAULT COLLATE utf8_general_ci;

GRANT ALL PRIVILEGES ON test.* TO pc@localhost IDENTIFIED BY 'pc';

USE test;

CREATE TABLE Message (
  id int(11) NOT NULL AUTO_INCREMENT,
  timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  message_type text,
  content text,
  author text,
  PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=latin1;

CREATE TABLE hibernate_sequence (
  next_val bigint(20) DEFAULT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;