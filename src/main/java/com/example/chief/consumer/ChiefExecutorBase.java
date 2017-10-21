package com.example.chief.consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.kinesis.connectors.interfaces.IKinesisConnectorPipeline;
import com.amazonaws.services.kinesis.metrics.interfaces.IMetricsFactory;
import com.example.chief.configuration.ChiefConfiguration;

public abstract class ChiefExecutorBase<T, U> implements Runnable {
    private static final Log LOG = LogFactory.getLog(ChiefExecutorBase.class);

    // Amazon Kinesis Client Library worker to process records
    protected Worker worker;

    /**
     * Initialize the Amazon Kinesis Client Library configuration and worker
     * 
     * @param kinesisConnectorConfiguration Amazon Kinesis connector configuration
     */
    protected void initialize(ChiefConfiguration configuration) {
        initialize(configuration, null);
    }

    /**
     * Initialize the Amazon Kinesis Client Library configuration and worker with metrics factory
     * 
     * @param kinesisConnectorConfiguration Amazon Kinesis connector configuration
     * @param metricFactory would be used to emit metrics in Amazon Kinesis Client Library
     */
    protected void
            initialize(ChiefConfiguration configuration, IMetricsFactory metricFactory) {
        KinesisClientLibConfiguration kinesisClientLibConfiguration =
                new KinesisClientLibConfiguration(configuration.APP_NAME,
                		configuration.KINESIS_INPUT_STREAM,
                		configuration.AWS_CREDENTIALS_PROVIDER,
                		configuration.WORKER_ID).withKinesisEndpoint(configuration.KINESIS_ENDPOINT)
                        .withFailoverTimeMillis(configuration.FAILOVER_TIME)
                        .withMaxRecords(configuration.MAX_RECORDS)
                        .withInitialPositionInStream(configuration.INITIAL_POSITION_IN_STREAM)
                        .withIdleTimeBetweenReadsInMillis(configuration.IDLE_TIME_BETWEEN_READS)
                        .withCallProcessRecordsEvenForEmptyRecordList(configuration.DEFAULT_CALL_PROCESS_RECORDS_EVEN_FOR_EMPTY_LIST)
                        .withCleanupLeasesUponShardCompletion(configuration.CLEANUP_TERMINATED_SHARDS_BEFORE_EXPIRY)
                        .withParentShardPollIntervalMillis(configuration.PARENT_SHARD_POLL_INTERVAL)
                        .withShardSyncIntervalMillis(configuration.SHARD_SYNC_INTERVAL)
                        .withTaskBackoffTimeMillis(configuration.BACKOFF_INTERVAL)
                        .withMetricsBufferTimeMillis(configuration.CLOUDWATCH_BUFFER_TIME)
                        .withMetricsMaxQueueSize(configuration.CLOUDWATCH_MAX_QUEUE_SIZE)
                        .withUserAgent(configuration.APP_NAME + ","
                                + configuration.KINESIS_CHIEF_USER_AGENT)
                        .withRegionName(configuration.REGION_NAME);

        if (!configuration.CALL_PROCESS_RECORDS_EVEN_FOR_EMPTY_LIST) {
            LOG.warn("The false value of callProcessRecordsEvenForEmptyList will be ignored. It must be set to true for the bufferTimeMillisecondsLimit to work correctly.");
        }

        if (configuration.IDLE_TIME_BETWEEN_READS > configuration.BUFFER_MILLISECONDS_LIMIT) {
            LOG.warn("idleTimeBetweenReads is greater than bufferTimeMillisecondsLimit. For best results, ensure that bufferTimeMillisecondsLimit is more than or equal to idleTimeBetweenReads ");
        }

        // If a metrics factory was specified, use it.
        if (metricFactory != null) {
            worker = new Worker.Builder()
            	    .recordProcessorFactory(getChiefRecordProcessorFactory())
            	    .config(kinesisClientLibConfiguration)
            	    .metricsFactory(metricFactory)
            	    .build();
            
        } else {
            worker = new Worker.Builder()
            	    .recordProcessorFactory(getChiefRecordProcessorFactory())
            	    .config(kinesisClientLibConfiguration)
            	    .build();
        }
        LOG.info(getClass().getSimpleName() + " worker created");
    }

    @Override
    public void run() {
        if (worker != null) {
            // Start Amazon Kinesis Client Library worker to process records
            LOG.info("Starting worker in " + getClass().getSimpleName());
            try {
                worker.run();
            } catch (Throwable t) {
                LOG.error(t);
                throw t;
            } finally {
                LOG.error("Worker " + getClass().getSimpleName() + " is not running.");
            }
        } else {
            throw new RuntimeException("Initialize must be called before run.");
        }
    }

    /**
     * This method returns a {@link ChiefRecordProcessorFactory} that contains the
     * appropriate {@link IKinesisConnectorPipeline} for the Amazon Kinesis Enabled Application
     * 
     * @return a {@link KinesisConnectorRecordProcessorFactory} that contains the appropriate
     *         {@link IKinesisConnectorPipeline} for the Amazon Kinesis Enabled Application
     */
    public abstract ChiefRecordProcessorFactory<T, U> getChiefRecordProcessorFactory();
}
