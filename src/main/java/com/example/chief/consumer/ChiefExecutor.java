package com.example.chief.consumer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.example.chief.configuration.ChiefConfiguration;

public abstract class ChiefExecutor<T, U> extends ChiefExecutorBase<T, U> {
 
    // Class variables
    protected final ChiefConfiguration config;
    private final Properties properties;
    
    /**
     * Create a new ChiefExecutor based on the provided configuration (*.propertes) file.
     * 
     * @param configFile
     *        The name of the configuration file to look for on the classpath
     */
    public ChiefExecutor(String configFile) {
        InputStream configStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFile);

        if (configStream == null) {
            String msg = "Could not find resource " + configFile + " in the classpath";
            throw new IllegalStateException(msg);
        }
        properties = new Properties();
        try {
            properties.load(configStream);
            configStream.close();
        } catch (IOException e) {
            String msg = "Could not load properties file " + configFile + " from classpath";
            throw new IllegalStateException(msg, e);
        }
        this.config = new ChiefConfiguration(properties, new DefaultAWSCredentialsProviderChain());

        // Initialize executor with configurations
        super.initialize(config);
    }
	    
}
