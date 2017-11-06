CREATE DATABASE chief;

USE chief;

CREATE TABLE orders (
	orderid VARCHAR(73) NOT NULL,
	orderdata TEXT DEFAULT NULL,
	processedtimestamp DATETIME DEFAULT NULL,
	PRIMARY KEY (`orderid`)
) ;

CREATE TABLE jobresults (
	orderid VARCHAR(73) NOT NULL,
	userid VARCHAR(255) NOT NULL,
	factoryid VARCHAR(255) NOT NULL,
	robotid VARCHAR(255) NOT NULL,
	jobstatus VARCHAR(255) NOT NULL,
	timestamp DATETIME DEFAULT NULL,
	PRIMARY KEY (`orderid`)
);

