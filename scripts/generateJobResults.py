#!/usr/bin/python
# This script will generate Json data on demand

#pip install mysql-connector-python-rf
import mysql.connector
import uuid, sys, time, csv, json, os, random
from time import gmtime, strftime

errUsage = "Usage: " + sys.argv[0] + " [number-runs] [rumber-rows]"
errEg = " -> eg: " + sys.argv[0] + " 10 100000"

# Basic Args Check and parse
if sys.argv[0] == "" and sys.argv[1] == "help":
    print(errUsage)
    print(errEg)
    exit(-1)

if len(sys.argv) != 3:
    print(errUsage)
    print(errEg)
    exit(-1)

numberRuns = int(sys.argv[1])
numberRows = int(sys.argv[2])

DB_USER = 'user'
DB_PASSWORD = '***********'
DB_HOST = 'chiefcluster.cluster-cexample.us-east-1.rds.amazonaws.com'
DB_NAME = 'chief'

conn = mysql.connector.connect(user=DB_USER, password=DB_PASSWORD, host=DB_HOST, database=DB_NAME)
cur = conn.cursor(prepared=True)

stmt = "SELECT * FROM orders WHERE processedtimestamp IS NULL LIMIT %s"
cur.execute(stmt, (numberRows,))                            # (2)

orderdatas = {}
for row in cur.fetchall():
    orderdatas[row[0].decode("utf-8")] = json.loads(row[1].decode("utf-8"))

cur.close
conn.close

factoryid = ["factory1","factory2","factory3","factory4"]
robotid = ["robot1","robot2","robot3","robot4","robot5","robot6"]
jobstatus = ["InProgress","Completed","Pending"]

targetDir = './JobResultsGeneratedData'
kplDir = './JobResultsKplWatch'
archiveDir = './JobResultsArchiveDir'

#Directory which the KPL watches
if not os.path.exists(kplDir):
      os.mkdir(kplDir)

#Directory which the KPL archives read file
if not os.path.exists(archiveDir):
       os.mkdir(archiveDir)

if __name__ == "__main__":
    # Generate data into multiple files into a sub directory called "generatedData"
    if not os.path.exists(targetDir):
      os.mkdir(targetDir)
    for y in xrange(numberRuns):
        timestart = time.strftime("%Y%m%d%H%M%S")
        destFile = str(uuid.uuid4()) + ".json"
        file_object = open(targetDir + "/" + destFile,"a")

        vallist = []
        def create_jobresults():
            x = 0
            for orderid, order in orderdatas.items():
                gen_factoryid = factoryid[random.randint(0, 3)]
                gen_robotid = robotid[random.randint(0, 5)]
                gen_jobstatus = jobstatus[random.randint(0, 2)]
                bust = random.uniform(2,50)
                weight = random.uniform(2,500)
                hip = random.uniform(2,120)
                waist = random.uniform(2,80)
                vallist.append("('" + orderid + "')")
                timestamp = strftime("%Y/%m/%d %H:%M:%S", gmtime())
                if x == 0:
                    file_object.write('[')
                        #'{'+"bust":  + str(bust) + '","weight": "' + str(weight) + '","hip": "' + str(hip)  + '","waist": "' + str(waist) + '},'
                #{"userId": "{{random.number(50)}}","orderId": "{{random.number(50)}}","factoryId": "{{random.number(50)}}","robotId": "{{random.number(50)}}","jobStatus": "{{random.arrayElement(["InProgress","Completed","Pending"])}}","timestamp": "{{date.utc("YYYY/MM/DD HH:mm:ss")}}"}
                file_object.write('{"userId": "' + order['userid'] + '","orderId": "' + orderid + '","factoryId": "' + gen_factoryid + '", "robotId": "' + gen_robotid + '","jobStatus": "' + gen_jobstatus + '","timestamp": "' + timestamp + '"}\n')

                if x == numberRows-1:
                    file_object.write(']')
                if x != numberRows-1:
                    file_object.write(',')
                x += 1

        if __name__ == "__main__":
            create_jobresults()
            file_object.close()

            vals = ",".join(vallist)
            conn = mysql.connector.connect(user=DB_USER, password=DB_PASSWORD, host=DB_HOST, database=DB_NAME)
            cur = conn.cursor()
            stmt = "INSERT INTO orders (orderid) VALUES{vals} ON DUPLICATE KEY UPDATE processedtimestamp = now();".format(vals=vals)
            #print(stmt)
            cur.execute(stmt)
            conn.commit()
            cur.close
            conn.close

            naptime=random.randint(3,40)
            print("generated " + str(numberRows) + " records into " + targetDir + "/" + destFile)
            print("sleeping for " + str(naptime) + " seconds")
            os.rename(targetDir+"/"+destFile, kplDir+"/"+destFile);
            time.sleep(naptime)

    print("\ngenerated: " + str(numberRuns) + " files, " + "with " + str(numberRows) + " records each\n" )
