package com.example.chief.consumer;

import com.example.chief.impl.ChiefJobResultNotifyS3Pipeline;
import com.example.chief.model.JobResult;

public class ChiefJobResultNotifyS3Executor extends ChiefExecutor<JobResult, byte[]> {

    private static final String CONFIG_FILE = "JobResultNotifyS3.properties";

    /**
     * Creates a new ChiefJobResultNotifyS3Executor.
     * Correspond to (6) KCL (Job results data data consumer)
     * 
     * @param configFile
     *        The name of the configuration file to look for on the classpath
     */
    public ChiefJobResultNotifyS3Executor(String configFile) {
        super(configFile);
    }

    @Override
    public ChiefRecordProcessorFactory<JobResult, byte[]>
    getChiefRecordProcessorFactory() {
        return new ChiefRecordProcessorFactory<JobResult, byte[]>(new ChiefJobResultNotifyS3Pipeline(), this.config);
    }

    /**
     * Main method to run the S3Executor.
     * 
     * @param args
     */
    public static void main(String[] args) {
    	ChiefExecutor<JobResult, byte[]> jobResultNotifyS3Executor = new ChiefJobResultNotifyS3Executor(CONFIG_FILE);
    	jobResultNotifyS3Executor.run();
    }
}
