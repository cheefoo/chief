package com.example.chief.consumer;

import com.example.chief.impl.ChiefOrderFactoryS3Pipeline;
import com.example.chief.model.Order;

public class ChiefOrderFactoryS3Executor extends ChiefExecutor<Order, byte[]> {

    private static final String CONFIG_FILE = "OrderFactoryS3.properties";

    /**
     * Creates a new ChiefOrderFactoryRDSExecutor.
     * Correspond to (2) KCL (Orders consumer in each factory)
     * 
     * @param configFile
     *        The name of the configuration file to look for on the classpath
     */
    public ChiefOrderFactoryS3Executor(String configFile) {
        super(configFile);
    }

    @Override
    public ChiefRecordProcessorFactory<Order, byte[]>
    getChiefRecordProcessorFactory() {
        return new ChiefRecordProcessorFactory<Order, byte[]>(new ChiefOrderFactoryS3Pipeline(), this.config);
    }

    /**
     * Main method to run the ChiefOrderFactoryRDSExecutor.
     * 
     * @param args
     */
    public static void main(String[] args) {
    	ChiefExecutor<Order, byte[]> orderFactoryS3Executor = new ChiefOrderFactoryS3Executor(CONFIG_FILE);
    	orderFactoryS3Executor.run();
    }
}
