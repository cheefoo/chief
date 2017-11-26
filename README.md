# chief Incorporated

## Overview
This is an end-to-end [AWS Kinesis Streams](https://aws.amazon.com/kinesis/streams/)  applications simulating a Just In Time Manufacturer of clothes utilizing Robo-Tailors.

The company "Chief Inc" is a "Just In Time" manufacturer of clothes on Demand and it prides itself on been able to complete a customer’s order within 24 hours. Customers of Chief Inc. can either submit their orders online by choosing the material, the design type from a list of templates as well as their sizes or go to a store and choose a custom design and custom sizes for their order.
An order submitted online looks like below:
{"id":"123", "gender":"M", "size":"L","material":"Linen", "design": "Aqua"}

An order submitted at a store can look like below
{{"id":"433", "gender":"F", "size": {"bust":"22", "weight":"140", "hip":"50", waist:"22"}}
,"material":"cotton", "design": "custom"}

Design Reference Table
{"gender":"male", "type":["aqua","black","visor","repeat","peculiar"]}
{"gender":"female", "type":["tourquiose","velvet","remebrance","relax","awesome"]}

Once the orders are submitted, the company will like the orders to be routed to robot-tailors that will handle jobs in real-time.
Chief Inc. does not keep any inventory of materials in his factory. Chief Inc has 4 robo-factories situated in each geographical zone in the US and 50 stores in the United States.

This simulation uses the [AWS Kinesis Producer Library (KPL)](http://docs.aws.amazon.com/streams/latest/dev/developing-producers-with-kpl.html) to send customer order records to a Kinesis stream called OrderStream.
The Robo Tailors consume from the stream using the [AWS Kinesis Client Library (KCL)](http://docs.aws.amazon.com/streams/latest/dev/developing-consumers-with-kcl.html)
and archive orders data to an [Amazon S3](http://docs.aws.amazon.com/AmazonS3/latest/dev/Welcome.html) bucket.
The Robot Tailors also use the KPL to send completed orders to an Amazon Kinesis Stream (JobResultsStream) whose KCL consumers then consume the records updates an elasticsearch cluster, notify customers and then archive results.


The application consists of 5 components:

1. Scripts to generate sample data records in the format required by the application(s)
2. Kinesis producer applications for orders and job results
3. Orders stream consumer KCL application that archives de-duplicated data records to S3
4. Orders stream consumer KCL application that archives de-duplicated data records to S3 and Elasticsearch
5. Job result stream consumer KCL application that archives de-duplicated data records to S3
6. Job result stream consumer KCL application that archives de-duplicated data record to Elasticsearch

## Architecture Diagrams:
![alt tag](https://github.com/rirakuchell/chief/blob/master/architecture-diagram.png)

## Requirements
1. An Amazon Web Services [Account](https://portal.aws.amazon.com/billing/signup#/start)
2. AWS CLI Installed and configured
3. After following the steps in the **Setting up the environment** section, you will have set up the following resources:
    - Two Amazon Kinesis Streams named ```OrdersStream``` and ```JobResultsStream```
    - Two IAM roles, Instance Profiles and [Policies](http://docs.aws.amazon.com/streams/latest/dev/controlling-access.html) required for the KCL and KPL instances
    - Two AWS EC2 Instances based on AmazonLinux with dependencies pre-installed
    - An Amazon Aurora cluster(for final de-dupe)
    - An Amazon Elasticsearch Service domain
    - An Amazon S3 bucket
4. When the KCL is initiated, DynamoDB tables for each applications are created

- Create tables for orders and job results in Aurora cluster like follwing SQL

## Setting up the environment
1. Create two Kinesis Streams using AWS CLI

  ```
  aws kinesis create-stream --stream-name OrdersStream --shard-count 4
  aws kinesis create-stream --stream-name JobResultsStream --shard-count 2
  ```

2. Create the Kinesis IAM roles required for EC2 Instances
  ```
    aws iam create-role \
    --role-name Chief-KPLRole \
    --assume-role-policy-document '{
        "Version": "2012-10-17",
        "Statement": [{
            "Sid": "",
            "Effect": "Allow",
            "Principal": {
                "Service": "ec2.amazonaws.com"
            },
            "Action": "sts:AssumeRole"
        }]
    }'

    aws iam create-role \
    --role-name Chief-KCLRole \
    --assume-role-policy-document '{
      "Version": "2012-10-17",
      "Statement": [{
          "Sid": "",
          "Effect": "Allow",
          "Principal": {
              "Service": "ec2.amazonaws.com"
          },
          "Action": "sts:AssumeRole"
      }]
    }'

  aws iam create-instance-profile --instance-profile-name Chief-KCLRole

  aws iam create-instance-profile --instance-profile-name Chief-KPLRole

  aws iam add-role-to-instance-profile --instance-profile-name Chief-KPLRole --role-name Chief-KPLRole

  aws iam add-role-to-instance-profile --instance-profile-name Chief-KCLRole --role-name Chief-KCLRole
  ```

3. Create the Kinesis IAM Policies  (Please replace the account ids with your own account id and the aws region with the region in which you will like your applications to run )
  ```
  aws iam create-policy \
  --policy-name Chief-KPLPolicy \
  --policy-document '{
      "Version": "2012-10-17",
      "Statement": [{
          "Effect": "Allow",
          "Action": ["kinesis:PutRecord","kinesis:PutRecords","kinesis:DescribeStream"],
          "Resource": ["arn:aws:kinesis:us-east-1:111122223333:stream/OrdersStream","arn:aws:kinesis:us-east-1:111122223333:stream/JobResultsStream"]
      },
      {
          "Sid": "Stmt1482832527000",
          "Effect": "Allow",
          "Action": ["cloudwatch:PutMetricData"],
          "Resource": ["*"]
      }
      ]
  }'

  aws iam create-policy \
  --policy-name Chief-KCLPolicy \
  --policy-document '{
      "Version": "2012-10-17",
      "Statement": [{
          "Effect": "Allow",
          "Action": ["kinesis:Get*"],
          "Resource": ["arn:aws:kinesis:us-east-1:111122223333:stream/OrdersStream","arn:aws:kinesis:us-east-1:111122223333:stream/JobResultsStream"]
      },
      {
            "Effect": "Allow",
            "Action": [
                "s3:*"
            ],
            "Resource": ["arn:aws:s3:::Chief-S3Bucket","arn:aws:s3:::<BUCKET_NAME>/*"]
        },
      {
          "Effect": "Allow",
          "Action": ["kinesis:DescribeStream"],
          "Resource": ["arn:aws:kinesis:us-east-1:111122223333:stream/OrdersStream","arn:aws:kinesis:us-east-1:111122223333:stream/JobResultsStream"]
      }, {
          "Effect": "Allow",
          "Action": ["kinesis:ListStreams"],
          "Resource": ["*"]
      }, {
          "Effect": "Allow",
          "Action": ["dynamodb:CreateTable", "dynamodb:DescribeTable", "dynamodb:Scan", "dynamodb:PutItem", "dynamodb:UpdateItem", "dynamodb:GetItem"],
          "Resource": ["arn:aws:dynamodb:us-east-1:111122223333:table/Chief*"]
      }, {
          "Sid": "Stmt1482832527000",
          "Effect": "Allow",
          "Action": ["cloudwatch:PutMetricData"],
          "Resource": ["*"]
      }]
  }'
  ```

4. Attach the Policies to the Roles
  ```
  aws iam attach-role-policy \
  --policy-arn "arn:aws:iam::111122223333:policy/Chief-KPLPolicy" \
  --role-name Chief-KPLRole

  aws iam attach-role-policy \
  --policy-arn "arn:aws:iam::111122223333:policy/Chief-KCLPolicy" \
  --role-name Chief-KCLRole
  ```

5. Create an Amazon Aurora cluster and take note of the cluster endpoint, username and password
First create Aurora DB cluster.
  ```
  aws rds create-db-cluster --db-cluster-identifier chiefcluster --engine aurora \
       --master-username user-name --master-user-password ******* \
       --db-subnet-group-name mysubnetgroup --vpc-security-group-ids sg-c7e5b0d2
  ```

  Please make sure to set the security group ```--vpc-security-group-ids``` is set to connect from KPL/KCL app/generator script instances.

  Next create DB instance for this cluster.
  ```
  aws rds create-db-instance --db-instance-identifier chiefinstance \
     --db-cluster-identifier chiefcluster --engine aurora --db-instance-class db.r3.large
  ```

  Also the Aurora cluster must be configured to be able to read orders and job results data from S3. Please refer to the following document to complete this.
 - http://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/AuroraMySQL.Integrating.LoadFromS3.html

6. Create an Amazon S3 bucket
  Please change the bucket name that you specified step 3.

  ```
  aws s3 mb s3://Chief-S3Bucket
  ```

7. Create an Amazon Set up Elasticsearch Service domain
  ```
aws es create-elasticsearch-domain --domain-name chief --elasticsearch-version 5.5 --elasticsearch-cluster-config InstanceType=m4.large.elasticsearch,InstanceCount=1 --ebs-options EBSEnabled=true,VolumeType=gp2,VolumeSize=10
  ```

8. Set up Elasticsearch domain access policy
  Set up Elasticsearch domain access policy IP addresses to connect from KPL/KCL app instances.

  ```
  aws es update-elasticsearch-domain-config --domain-name chief --access-policies '{"Version": "2012-10-17", "Statement": [{"Action": "es:ESHttp*","Principal":"*","Effect": "Allow", "Condition": {"IpAddress":{"aws:SourceIp":["192.0.2.0/32"]}}}]}'
  ```
9. Create a Bootstrap script to automate the installation of the dependencies on newly launched instances
  ```
  cat <<EOF > Bootstrap.sh
  #!/bin/bash
  sudo yum install -y java-1.8.0-* git gcc-c++ make
  sudo yum remove -y java-1.7.0-*
  curl --silent --location https://rpm.nodesource.com/setup_6.x | sudo bash -
  sudo yum install mysql -y
  sudo pip install faker
  sudo pip install --egg mysql-connector-python-rf
  cd /home/ec2-user
  wget http://mirrors.whoishostingthis.com/apache/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.zip
  unzip apache-maven-3.3.9-bin.zip
  echo "export PATH=\$PATH:/home/ec2-user/apache-maven-3.3.9/bin" >> .bashrc
  git clone https://github.com/rirakuchell/chief.git
  mkdir ./chief/logs
  chown -R ec2-user ./chief
  echo export DB_USER=dbuser >> /etc/profile
  echo export DB_PASSWORD=******* >> /etc/profile
  echo export DB_HOST=chiefcluster.cluster-example.us-east-1.rds.amazonaws.com >> /etc/profile
  echo export DB_NAME=chief >> /etc/profile
  echo export BUCKET_NAME=Chief-S3Bucket >> /etc/profile
  echo export ORDERS_PREFIX=ChiefOrderS3 >> /etc/profile
  echo export JOBRESULTS_PREFIX=ChiefJobResultNotifyS3 >> /etc/profile
  EOF
  ```
  Please don't forget to modify environment variables. The descriptions for environment variables are follwing.

  | Key           | Default                                        | Description                                                                     |
  | :------------ | :--------------------------------------------- | :------------------------------------------------------------------------------ |
  | DB_USER    | dbuser| Database user for Aurora cluster. |
  | DB_PASSWORD   | \******* | Database password for Aurora cluster. |
  | DB_HOST    | chiefcluster.cluster-example.us-east-1.rds.amazonaws.com | Cluster endpoint of Aurora cluster.|
  | BUCKET_NAME     | Chief-S3Bucket | S3 bucket name where the file exists for reading into the Aurora cluster. Please change the bucket name that you specified step 3.|
  | ORDERS_PREFIX     | ChiefOrderS3 | S3 prefix where the file exists for reading into the Aurora cluster.|
  | JOBRESULTS_PREFIX     | ChiefJobResultNotifyS3 | S3 prefix where the file exists for reading into the Aurora cluster.|

10. Please note that image-id given in below command belongs to us-east-1, if you are launching in a different region please look up the image-id for that region [AWS Linux AMI IDs](https://aws.amazon.com/amazon-linux-ami/). Take note of the returned "InstanceId" after launching each instance in order to create tags

  ```
  aws ec2 run-instances \
  --image-id ami-9be6f38c \
  --key-name sshkeypair \
  --security-groups default \
  --instance-type m3.large \
  --iam-instance-profile Name="Chief-KPLRole" \
  --user-data file://Bootstrap.sh

  aws ec2 create-tags --resources i-000d3b6d9fexample --tags Key=Name,Value="Chief-KPLInstance"

  aws ec2 run-instances \
  --image-id ami-9be6f38c \
  --key-name sshkeypair \
  --security-groups default \
  --instance-type m3.large \
  --iam-instance-profile Name="Chief-KCLRole" \
  --user-data file://Bootstrap.sh

  aws ec2 create-tags --resources i-0879e274caexample --tags Key=Name,Value="Chief-KCLInstance"
  ```

11. Dont forget to modify the default security group to allow ssh access.

## Running a Sample

1. SSH into the KPL Instance and edit Edit the *.properties file for KCL application in ```~/chief/src/main/resources/``` folder. Please change the values for your own AWS resources.
 - ```OrderProducer.properties``` : The setting of KCL application (2) in diagram

  | Key           | Default                                        | Description                                                                     |
  | :------------ | :--------------------------------------------- | :------------------------------------------------------------------------------ |
  | kinesisOutputStream    | OrdersStream| The destination Kinesis Stream name for producer. |
  | regionName   | us-east-1                                           | AWS region name for Kinesis Streams |
  | dataFolder    | /home/ec2-user/chief/kplWatch| The folder path that generated data is placed.|
  | producerDuration     | 7200 | The duration of running producer program.|

 - ```JobResultsProducer.properties``` : The setting of KCL application (4) in diagram

  | Key           | Default                                        | Description                                                                     |
  | :------------ | :--------------------------------------------- | :------------------------------------------------------------------------------ |
  | kinesisOutputStream    | JobResultsStream| The destination Kinesis Stream name for producer. |
  | regionName   | us-east-1                                           | AWS region name for Kinesis Streams |
  | dataFolder    | /home/ec2-user/chief/JobResultsKplWatch| The folder path that generated data is placed.|
  | producerDuration     | 7200 | The duration of running producer program.|
2. Edit the log4j.properties file for log4j in ```~/chief/src/main/resources/``` folder.
3. Login to the Aurora DB instance from the ec2 instance and create the orders and jobresults table by using the ddl chiefdb.sql located in ~/chief/src/main/resources/chiefdb.sql
  ```
  mysql -h ${DB_HOST} -u ${DB_USER} -p${DB_PASSWORD} mysql < ~/chief/src/main/resources/chiefdb.sql
  ```
4. SSH into the KCL Instance and edit Edit the *.properties file for KCL application in ```~/chief/src/main/resources/``` folder. Please change the values for your own AWS resources and KCL application names. For details of each property names, please see the actual property files.
 - ```OrderFactoryS3.properties``` : The setting of KCL application (2) in diagram
 - ```OrderElasticsearchS3.properties``` : The setting of KCL application (4) in diagram
 - ```JobResultElasticsearch.properties``` : The setting of KCL application (5) in diagram
 - ```JobResultNotifyS3.properties``` : The setting of KCL application (6) in diagram
5. Edit the log4j.properties file for log4j in ```~/chief/src/main/resources/``` folder.
6. On KPL Instance, Build the program using Maven
```
cd ~/chief
mvn clean compile assembly:single
```
7. On KPL Instance, Generate orders data using python script and put into Orders Stream using chief-producer.
  First generate online orders and store orders.
  ```
  python ./scripts/generateOnlineOrders.py 1 1000
  python ./scripts/generateStoreOrders.py 1 1000
  ```
  Next put it into OrdersStream Kinesis stream.
  ```
  nohup bash -c \
  "(java -cp ./target/Kinesis-Chief-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.example.chief.producer.ChiefOrderProducerExecutor > ~/chief/logs/ChiefOrderProducerExecutor.log) \
   &> ~/chief/logs/ChiefOrderProducerExecutor.log" &
  ```
  Once this is done, check Kinesis Streams metrics(e.g. PutRecords.Records) to confirm the put is successfully. The generated data will be backed up to "backup" folder in "dataFolder" property.

8. On KCL Instance, Build the program using Maven.
  ```
  cd ~/chief
  mvn clean compile assembly:single
  ```
9. On KCL Instance, Run orders stream consumer (2) in diagram
  ```
  nohup bash -c \
  "(java -cp ./target/Kinesis-Chief-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.example.chief.consumer.ChiefOrderFactoryS3Executor > ~/chief/logs/ChiefOrderFactoryS3Executor.log) \
   &> ~/chief/logs/ChiefOrderFactoryS3Executor.log" &
  ```
  The order data will be consumed from Kinesis Stream and put to S3 like key ```s3://bucket/ChiefOrderS3/2017/10/18/16/shardId-000000000000-49577848089610314880664684998647902014807844560960487426```

10. On KCL Instance, Run orders stream consumer (4) in diagram
  ```
  nohup bash -c \
  "(java -cp ./target/Kinesis-Chief-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.example.chief.consumer.ChiefOrderElasticsearchS3Executor > ~/chief/logs/ChiefOrderElasticsearchS3Executor.log) \
   &> ~/chief/logs/ChiefOrderElasticsearchS3Executor.log" &
  ```
  The order data will be put to Elasticsearch.

11. On KCL instance, load orders and job results data periodically from S3 to Aurora for final de-duplication. This can be done with following cron settings
  ```
  crontab -e
  ```
  Specify follwing cron setting. "loadS3DataToAurora.py" loads data from S3 to Aurora cluster.
  ```
  */5 * * * * . /etc/profile;python /home/ec2-user/chief/scripts/loadS3DataToAurora.py > /home/ec2-user/chief/logs/loadS3DataToAurora.txt 2>&1
  ```

12. On KPL instance,Generate job results data using python script and put into Job results Stream using chief-producer.
  ```
  python ./scripts/generateJobResults.py 1 1000
  ```
  generateJobResults.py connects Aurora cluster to load orders data. Environment variables that set by userdata script are used as DB connection configuration(e.g. DB_USER).
  ```
  nohup bash -c \
  "(java -cp ./target/Kinesis-Chief-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.example.chief.producer.ChiefJobResultsProducerExecutor > ~/chief/logs/ChiefJobResultsProducerExecutor.log) \
   &> ~/chief/logs/ChiefJobResultsProducerExecutor.log" &
  ```
13. On KCL instance, Run job results stream consumer (5) in diagram
  ```
  nohup bash -c \
  "(java -cp ./target/Kinesis-Chief-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.example.chief.consumer.ChiefJobResultElasticsearchExecutor > ~/chief/logs/ChiefJobResultElasticsearchExecutor.log) \
  &> ~/chief/logs/ChiefJobResultElasticsearchExecutor.log" &
  ```

14. On KCL instance, Run job results stream consumer (6) in diagram
  ```
  nohup bash -c \
  "(java -cp ./target/Kinesis-Chief-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.example.chief.consumer.ChiefJobResultNotifyS3Executor > ~/chief/logs/ChiefJobResultNotifyS3Executor.log) \
   &> ~/chief/logs/ChiefJobResultNotifyS3Executor.log" &
  ```

15. Open https://search-domainname-example.us-east-1.es.amazonaws.com/_plugin/kibana/ from your browser. This URL is the endpoint of Elasticsearch Service domain. Make sure the access policy of domain is open from your environment.

At first create two index patterns "chieforders-\*" and "chiefjobresults-\*". Then you can query orders and job results put from KCL application.

Following screenshot is sample dashboard.
![alt tag](https://github.com/rirakuchell/chief/blob/master/chief-samplegraph.png)

I made following json file in "essavedobjects" folder for creating visualize and dashboard. You can import them "Management" -> "Saved Objects"
- ```genderchart.json```: how many orders are handled per hour for a gender type
- ```genderFdesigns.json```: how many custom orders are of gender type female and design type velvet per hour
- ```CompletedJobPerFactoryPerRobot.json```: completed requests per hour per factory per robot
- ```ChiefDashboard.json```: a sample dashboard

