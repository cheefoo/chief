package com.example.chief.consumer;

import com.example.chief.impl.ChiefJobResultElasticsearchPipeline;
import com.example.chief.model.JobResult;

public class ChiefJobResultElasticsearchExecutor extends ChiefExecutor<JobResult, byte[]> {

    private static final String CONFIG_FILE = "JobResultElasticsearch.properties";

    /**
     * Creates a new ChiefJobResultElasticsearchExecutor.
     * Correspond to (5) KCL (Job results data data consumer for dashboard)
     * 
     * @param configFile
     *        The name of the configuration file to look for on the classpath
     */
    public ChiefJobResultElasticsearchExecutor(String configFile) {
        super(configFile);
    }

    @Override
    public ChiefRecordProcessorFactory<JobResult, byte[]>
    getChiefRecordProcessorFactory() {
        return new ChiefRecordProcessorFactory<JobResult, byte[]>(new ChiefJobResultElasticsearchPipeline(), this.config);
    }

    /**
     * Main method to run the S3Executor.
     * 
     * @param args
     */
    public static void main(String[] args) {
    	ChiefExecutor<JobResult, byte[]> jobResultElasticsearchExecutor = new ChiefJobResultElasticsearchExecutor(CONFIG_FILE);
    	jobResultElasticsearchExecutor.run();
    }
}
