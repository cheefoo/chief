CREATE TABLE orders (
	orderid VARCHAR(73) NOT NULL,
	orderdata TEXT DEFAULT NULL,
	processedtimestamp DATETIME DEFAULT NULL,
	PRIMARY KEY (`orderid`)
) ;

-- http://docs.aws.amazon.com/ja_jp/AmazonRDS/latest/UserGuide/Aurora.LoadFromS3.html
LOAD DATA FROM S3 PREFIX 's3://bucket/ChiefOrderS3/2017/10/18/16/'
IGNORE
INTO TABLE orders
(orderid, orderdata);

CREATE TABLE jobresults (
	orderid VARCHAR(73) NOT NULL,
	userid VARCHAR(255) NOT NULL,
	factoryid VARCHAR(255) NOT NULL,
	robotid VARCHAR(255) NOT NULL,
	jobstatus VARCHAR(255) NOT NULL,
	timestamp DATETIME DEFAULT NULL,
	PRIMARY KEY (`orderid`)
);

LOAD DATA FROM S3 PREFIX 's3://bucket/ChiefJobResultNotifyS3/2017/10/17/17/'
IGNORE
INTO TABLE jobresults
(orderid, userid, factoryid, robotid, jobstatus, timestamp);

