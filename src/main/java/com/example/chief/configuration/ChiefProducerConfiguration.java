package com.example.chief.configuration;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.auth.AWSCredentialsProvider;

public class ChiefProducerConfiguration {
    private static final Log LOG = LogFactory.getLog(ChiefProducerConfiguration.class);
    public static final String KINESIS_CHIEF_USER_AGENT = "kinesis-chief-producer-0.0.1";

    // Connector App Property Keys
    public static final String PROP_KINESIS_OUTPUT_STREAM = "kinesisOutputStream";
    public static final String PROP_REGION_NAME = "regionName";
    public static final String PROP_ORIGINAL_DATA_FOLDER = "dataFolder";
    public static final String PROP_PRODUCER_DURATION = "producerDuration"; 
   
    // Default Amazon Kinesis Constants
    public static final String DEFAULT_KINESIS_OUTPUT_STREAM = "kinesisOutputStream";
    
    public static final String DEFAULT_ORIGINAL_DATA_FOLDER = null;
    public static final int DEFAULT_PRODUCER_DURATION = 3600;
    
    public static final String DEFAULT_REGION_NAME = "us-east-1";

    // Configurable program variables
    public final AWSCredentialsProvider AWS_CREDENTIALS_PROVIDER;

    public final String KINESIS_OUTPUT_STREAM;
    public final String ORIGINAL_DATA_FOLDER;
    public final int PRODUCER_DURATION;

    public final String REGION_NAME;
    /**
     * Configure the connector application with any set of properties that are unique to the application. Any
     * unspecified property will be set to a default value.
     * 
     * @param properties
     *        the System properties that will be used to configure KinesisConnectors
     */
    public ChiefProducerConfiguration(Properties properties, AWSCredentialsProvider credentialsProvider) {
        AWS_CREDENTIALS_PROVIDER = credentialsProvider;

        KINESIS_OUTPUT_STREAM = properties.getProperty(PROP_KINESIS_OUTPUT_STREAM, DEFAULT_KINESIS_OUTPUT_STREAM);
        ORIGINAL_DATA_FOLDER = properties.getProperty(PROP_ORIGINAL_DATA_FOLDER, DEFAULT_ORIGINAL_DATA_FOLDER);
        PRODUCER_DURATION = getIntegerProperty(PROP_PRODUCER_DURATION, DEFAULT_PRODUCER_DURATION, properties);

        REGION_NAME = properties.getProperty(PROP_REGION_NAME, DEFAULT_REGION_NAME);
    }

    private int getIntegerProperty(String property, int defaultValue, Properties properties) {
        String propertyValue = properties.getProperty(property, Integer.toString(defaultValue));
        try {
            return Integer.parseInt(propertyValue.trim());
        } catch (NumberFormatException e) {
            LOG.error(e);
            return defaultValue;
        }
    }

}