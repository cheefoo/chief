#!/usr/bin/python
# This script will load S3 data to Aurora cluster

#pip install mysql-connector-python-rf
import mysql.connector
import uuid, sys, time, csv, json, os, random
from time import gmtime, strftime
import datetime

DB_USER = os.getenv("DB_USER")
DB_PASSWORD = os.getenv("DB_PASSWORD")
DB_HOST = os.getenv("DB_HOST")
DB_NAME = os.getenv("DB_NAME")

BUCKET_NAME = os.getenv("BUCKET_NAME")
ORDERS_PREFIX = os.getenv("ORDERS_PREFIX")
JOBRESULTS_PREFIX = os.getenv("JOBRESULTS_PREFIX")

s3_path = 's3://' + BUCKET_NAME + '/'
orders_s3_path = ''
jobresults_s3_path = ''

if ORDERS_PREFIX is not None:
    orders_s3_path = s3_path + ORDERS_PREFIX + '/'
if JOBRESULTS_PREFIX is not None:
    jobresults_s3_path = s3_path + JOBRESULTS_PREFIX + '/'

now = datetime.datetime.now()
nowminus1 = now - datetime.timedelta(hours=1)
prefix_now = now.strftime("%Y/%m/%d/%H/")
prefix_nowminus1 = nowminus1.strftime("%Y/%m/%d/%H/")

stmt_orders = """
LOAD DATA FROM S3 PREFIX '{s3path}'
IGNORE
INTO TABLE orders
(orderid, orderdata);
"""

stmt_jobresults = """
LOAD DATA FROM S3 PREFIX '{s3path}'
IGNORE
INTO TABLE jobresults
(orderid, userid, factoryid, robotid, jobstatus, timestamp);
"""

stmt_orders_now = stmt_orders.format(s3path=orders_s3_path + prefix_now)
stmt_orders_nowminus1 = stmt_orders.format(s3path=orders_s3_path + prefix_nowminus1)

stmt_jobresults_now = stmt_jobresults.format(s3path=jobresults_s3_path + prefix_now)
stmt_jobresults_nowminus1 = stmt_jobresults.format(s3path=jobresults_s3_path + prefix_nowminus1)

conn = mysql.connector.connect(user=DB_USER, password=DB_PASSWORD, host=DB_HOST, database=DB_NAME)
cur = conn.cursor()

try:
    cur.execute(stmt_orders_nowminus1)
    conn.commit()
except mysql.connector.errors.DatabaseError as err:
    print("DatabaseError: {0}".format(err))

try:
    cur.execute(stmt_jobresults_nowminus1)
    conn.commit()
except mysql.connector.errors.DatabaseError as err:
    print("DatabaseError: {0}".format(err))

try:
    cur.execute(stmt_orders_now)
    conn.commit()
except mysql.connector.errors.DatabaseError as err:
    print("DatabaseError: {0}".format(err))

try:
    cur.execute(stmt_jobresults_now)
    conn.commit()
except mysql.connector.errors.DatabaseError as err:
    print("DatabaseError: {0}".format(err))

cur.close
conn.close
