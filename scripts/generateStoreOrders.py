#!/usr/bin/python
# This script will generate Json data on demand
#{
#    "userid": "taylor37@yahoo.com",
#    "gender": "F",
#    "size": "S",
#    "material": "linen"
#    "design": "custom"
#    "activityTimestamp": "2016-12-07 11:46:29"
#}

from faker import Factory
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

size = ["S", "M", "L", "XL", "2XL", "3XL"]
material = ["linen", "cotton", "lace", "polyester", "denim", "nylon", "fur", "leather"]
design = ["custom"]
sex = ["M", "F", "O"]
targetDir = './generatedData'
kplDir = './kplWatch'
archiveDir = './archiveDir'

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

        def create_names(fake):
            for x in range(numberRows):
                genUserID = fake.email()
                genSex = sex[random.randint(0, 2)]
                genSize = size[random.randint(0, 5)]
                genMaterial = material[random.randint(0,7)]
                bust = random.uniform(2,50)
                weight = random.uniform(2,500)
                hip = random.uniform(2,120)
                waist = random.uniform(2,80)
                genActivityTimestamp = strftime("%Y/%m/%d %H:%M:%S", gmtime())
                if x == 0:
                    file_object.write('[')
                        #'{'+"bust":  + str(bust) + '","weight": "' + str(weight) + '","hip": "' + str(hip)  + '","waist": "' + str(waist) + '},'
                tailor = '{"bust": "' + str(bust) + '","weight": "' + str(weight) + '","hip": "' + str(hip)  + '","waist": "' + str(waist) + '"},'
                file_object.write('{"userid": "' + genUserID + '","gender": "' + genSex + '","size": ' + tailor + '"material": "' + genMaterial + '","design": "' + "custom" + '","activityTimestamp": "' + genActivityTimestamp + '"}\n')

                if x == numberRows-1:
                    file_object.write(']')
                if x != numberRows-1:
                    file_object.write(',')


        if __name__ == "__main__":
            fake = Factory.create()
            create_names(fake)
            file_object.close()
            naptime=random.randint(3,40)
            print "generated " + str(numberRows) + " records into " + targetDir + "/" + destFile
            print "sleeping for " + str(naptime) + " seconds"
            os.rename(targetDir+"/"+destFile, kplDir+"/"+destFile);
            time.sleep(naptime)

    print("\ngenerated: " + str(numberRuns) + " files, " + "with " + str(numberRows) + " records each\n" )
