# chief

## Overview
This is an end-to-end [AWS Kinesis Streams](https://aws.amazon.com/kinesis/streams/) processing example using the [AWS Kinesis Producer Library (KPL)](http://docs.aws.amazon.com/streams/latest/dev/developing-producers-with-kpl.html) to send records to a Kinesis stream, consume from the stream using the [AWS Kinesis Client Library (KCL)](http://docs.aws.amazon.com/streams/latest/dev/developing-consumers-with-kcl.html) and archive data to an [Amazon S3](http://docs.aws.amazon.com/AmazonS3/latest/dev/Welcome.html) bucket. Additionally, a second KCL consumer de-duplicates the consumed records and provides realtime data updates to a web front-end.

The application consists of 5 components:

1. Scripts to generate data in the format required by the application(s)
2. Kinesis producer applications for orders and job results
3. Orders stream consumer KCL application that archives de-duped datas to S3
4. Orders stream consumer KCL application that archives de-duped datas to S3 and Elasticsearch
5. Job result stream consumer KCL application that archives de-duped datas to S3
6. Job result stream consumer KCL application that archives de-duped datas to Elasticsearch

## Architecture Diagrams:
![alt tag](https://github.com/rirakuchell/chief/blob/master/Archie1.png)

## Requirements
1. An Amazon Web Services [Account](https://portal.aws.amazon.com/billing/signup#/start)
2. AWS CLI Installed and configured
3. After following the steps in the **Setting up the environment** section, you will have set up the following resources:  
  3.1. Two Amazon Kinesis Streams named ```OrdersStream``` and ```JobResultsStream```
  3.2. Two IAM roles, Instance Profiles and [Policies](http://docs.aws.amazon.com/streams/latest/dev/controlling-access.html) required for the KCL and KPL instances
  3.3. Two AWS EC2 Instances based on AmazonLinux with dependencies pre-installed
  3.3. An Amazon Aurora cluster(for final de-dupe)
 - The Aurora cluster must be configured to be able to read orders and job results data from S3. Please refer to the following document to complete this.
 - http://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Aurora.LoadFromS3.html
  3.4. An Amazon Elasticsearch Service domain
  3.5. An Amazon S3 bucket
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

3. Create the Kinesis IAM Policies  (Please replace the account ids with your own account id)
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
            "Resource": ["arn:aws:s3:::ChiefS3Bucket-","arn:aws:s3:::<BUCKET_NAME>/*"]
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

5. Create a Bootstrap script to automate the installation of the dependencies on newly launched instances 
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
  EOF
  ```

6. Create an Amazon Aurora cluster and take note of the cluster endpoint, username and password
  ```
aws rds create-db-cluster --db-cluster-identifier chiefcluster --engine aurora \
     --master-username user-name --master-user-password ******* \
     --db-subnet-group-name mysubnetgroup --vpc-security-group-ids sg-c7e5b0d2
  ```

  Please make sure to set the security group ```--vpc-security-group-ids``` is set to connect from KPL/KCL app/generator script instances.

7. Create an Amazon S3 bucket
  Please change the bucket name that you specified step 3.

  ```
  aws s3 mb s3://Chief-S3Bucket
  ```

8. Create an Amazon Set up Elasticsearch Service domain
  ```
aws es create-elasticsearch-domain --domain-name chief --elasticsearch-version 5.5 --elasticsearch-cluster-config InstanceType=m4.large.elasticsearch,InstanceCount=1 --ebs-options EBSEnabled=true,VolumeType=gp2,VolumeSize=10
  ```

9. Set up Elasticsearch domain access policy
  Set up Elasticsearch domain access policy IP addresses to connect from KPL/KCL app instances.
  
  ```
  aws es update-elasticsearch-domain-config --domain-name chief --access-policies '{"Version": "2012-10-17", "Statement": [{"Action": "es:ESHttp*","Principal":"*","Effect": "Allow", "Condition": {"IpAddress":{"aws:SourceIp":["192.0.2.0/32"]}}}]}'
  ```

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



```
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
```

## Running a Sample

1. Edit the *.properties file for KCL application in ```/src/main/resources``` folder. Please change the values for your own AWS resources and KCL application names.
 - OrderFactoryS3.properties : The setting of KCL application (2) in diagram
 - OrderElasticsearchS3.properties : The setting of KCL application (4) in diagram
 - JobResultElasticsearch.properties : The setting of KCL application (5) in diagram
 - JobResultNotifyS3.properties : The setting of KCL application (6) in diagram
2. Edit the *.properties file for log4j in ```/src/main/resources``` folder.
 - log4j.properties : log4j property for KCL application
3. Build the program using Maven
```
cd chief
mvn clean compile assembly:single
```
3. Generate orders data using python script and put into Orders Stream using chief-producer.
```

./target/Kinesis-Chief-0.0.1-SNAPSHOT-jar-with-dependencies.jar

```

4. Run orders stream consumer (2) in diagram
```
java -cp Kinesis-Chief-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.example.chief.consumer.ChiefOrderFactoryS3Executor
```
The order data will be put to S3 like key 's3://bucket/ChiefOrderS3/2017/10/18/16/shardId-000000000000-49577848089610314880664684998647902014807844560960487426'

5. Run orders stream consumer (4) in diagram
```
java -cp Kinesis-Chief-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.example.chief.consumer.ChiefOrderElasticsearchS3Executor
```
The order data will be put to Elasticsearch.
6. Load orders data periodically from S3 to Aurora for final de-duplication. This can be done with follwing SQL.
```
LOAD DATA FROM S3 PREFIX 's3://bucket/ChiefOrderS3/2017/10/18/16/'
IGNORE
INTO TABLE orders
(orderid, orderdata);
```
7. Generate job results data using python script and put into Job results Stream using chief-producer.

8. Run job results stream consumer (5) in diagram
```
java -cp Kinesis-Chief-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.example.chief.consumer.ChiefJobResultElasticsearchExecutor
```

9. Run job results stream consumer (6) in diagram
```
java -cp Kinesis-Chief-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.example.chief.consumer.ChiefJobResultNotifyS3Executor
```
10. Load job results data periodically from S3 to Aurora for final de-duplication. This can be done with follwing SQL.
```
LOAD DATA FROM S3 PREFIX 's3://bucket/ChiefJobResultNotifyS3/2017/10/17/17/'
IGNORE
INTO TABLE jobresults
(orderid, userid, factoryid, robotid, jobstatus, timestamp);
```
