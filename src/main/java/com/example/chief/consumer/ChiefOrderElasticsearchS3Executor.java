package com.example.chief.consumer;

import com.example.chief.impl.ChiefOrderElasticsearchS3Pipeline;
import com.example.chief.model.Order;

public class ChiefOrderElasticsearchS3Executor extends ChiefExecutor<Order, byte[]> {

    private static final String CONFIG_FILE = "OrderElasticsearchS3.properties";

    /**
     * Creates a new ChiefOrderElasticsearchS3Executor.
     * Correspond to (4) KCL (Orders data consumer for dashboard)
     * 
     * @param configFile
     *        The name of the configuration file to look for on the classpath
     */
    public ChiefOrderElasticsearchS3Executor(String configFile) {
        super(configFile);
    }

    @Override
    public ChiefRecordProcessorFactory<Order, byte[]>
    getChiefRecordProcessorFactory() {
        return new ChiefRecordProcessorFactory<Order, byte[]>(new ChiefOrderElasticsearchS3Pipeline(), this.config);
    }

    /**
     * Main method to run the S3Executor.
     * 
     * @param args
     */
    public static void main(String[] args) {
    	ChiefExecutor<Order, byte[]> orderElasticsearchS3Executor = new ChiefOrderElasticsearchS3Executor(CONFIG_FILE);
        orderElasticsearchS3Executor.run();
    }
}
