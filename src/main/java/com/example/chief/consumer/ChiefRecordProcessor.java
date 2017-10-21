package com.example.chief.consumer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.KinesisClientLibDependencyException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason;
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput;
import com.amazonaws.services.kinesis.connectors.UnmodifiableBuffer;
import com.amazonaws.services.kinesis.connectors.interfaces.*;
import com.amazonaws.services.kinesis.model.Record;
import com.example.chief.configuration.ChiefConfiguration;
import com.example.chief.interfaces.IChiefEmitter;

public class ChiefRecordProcessor<T, U> implements IRecordProcessor  {

    private final IChiefEmitter<U> emitter;
    private final ITransformerBase<T, U> transformer;
    private final IFilter<T> filter;
    private final IBuffer<T> buffer;
    private final int retryLimit;
    private final long backoffInterval;
    private boolean isShutdown = false;

    private static final Log LOG = LogFactory.getLog(ChiefRecordProcessor.class);

    private String shardId;

    /**
     * T U are specified by RecordProcessorFactory
     * T is MessageModel(like Order,JobResults)
     * U is (like byte[])
     * 
     * @see also https://github.com/awslabs/amazon-kinesis-connectors/blob/master/samples/src/main/java/samples/s3/S3Executor.java
     * 
     * @param buffer
     * @param filter
     * @param emitter
     * @param transformer
     * @param configuration
     */
    public ChiefRecordProcessor(IBuffer<T> buffer,
            IFilter<T> filter,
            IChiefEmitter<U> emitter,
            ITransformerBase<T, U> transformer,
            ChiefConfiguration configuration) {
        if (buffer == null || filter == null || emitter == null || transformer == null) {
            throw new IllegalArgumentException("buffer, filter, emitter, and transformer must not be null");
        }
        this.buffer = buffer;
        this.filter = filter;
        this.emitter = emitter;
        this.transformer = transformer;
        // Limit must be greater than zero
        if (configuration.RETRY_LIMIT <= 0) {
            retryLimit = 1;
        } else {
            retryLimit = configuration.RETRY_LIMIT;
        }
        this.backoffInterval = configuration.BACKOFF_INTERVAL;
    }

    @Override
    public void initialize(InitializationInput initializationInput) {
        this.shardId = initializationInput.getShardId();
        LOG.info("Initializing ChiefRecordProcessor for shard: " + this.shardId);
    }

    @Override
    public void processRecords(ProcessRecordsInput processRecordsInput) {
        // Note: This method will be called even for empty record lists. This is needed for checking the buffer time
        // threshold.
        if (isShutdown) {
            LOG.warn("processRecords called on shutdown record processor for shardId: " + shardId);
            return;
        }
        if (shardId == null) {
            throw new IllegalStateException("Record processor not initialized");
        }

        // Transform each Amazon Kinesis Record and add the result to the buffer
        for (Record record : processRecordsInput.getRecords()) {
            try {
                if (transformer instanceof ITransformer) {
                    ITransformer<T, U> singleTransformer = (ITransformer<T, U>) transformer;
                    filterAndBufferRecord(singleTransformer.toClass(record), record);
                } else if (transformer instanceof ICollectionTransformer) {
                    ICollectionTransformer<T, U> listTransformer = (ICollectionTransformer<T, U>) transformer;
                    Collection<T> transformedRecords = listTransformer.toClass(record);
                    for (T transformedRecord : transformedRecords) {
                        filterAndBufferRecord(transformedRecord, record);
                    }
                } else {
                    throw new RuntimeException("Transformer must implement ITransformer or ICollectionTransformer");
                }
            } catch (IOException e) {
                LOG.error(e);
            }
        }

        if (buffer.shouldFlush()) {
            List<U> emitItems = transformToOutput(buffer.getRecords());
            emit(processRecordsInput.getCheckpointer(), emitItems);
        }
    }

    private void filterAndBufferRecord(T transformedRecord, Record record) {
        if (filter.keepRecord(transformedRecord)) {
            buffer.consumeRecord(transformedRecord, record.getData().array().length, record.getSequenceNumber());
        }
    }

    private List<U> transformToOutput(List<T> items) {
        List<U> emitItems = new ArrayList<U>();
        for (T item : items) {
            try {
                emitItems.add(transformer.fromClass(item));
            } catch (IOException e) {
                LOG.error("Failed to transform record " + item + " to output type", e);
            }
        }
        return emitItems;
    }

    private void emit(IRecordProcessorCheckpointer checkpointer, List<U> emitItems) {
        List<U> unprocessed = new ArrayList<U>(emitItems);
        try {
            for (int numTries = 0; numTries < retryLimit; numTries++) {
                unprocessed = emitter.emit(new UnmodifiableBuffer<U>(buffer, unprocessed), shardId);
                if (unprocessed.isEmpty()) {
                    break;
                }
                try {
                    Thread.sleep(backoffInterval);
                } catch (InterruptedException e) {
                }
            }
            if (!unprocessed.isEmpty()) {
                emitter.fail(unprocessed);
            }
            final String lastSequenceNumberProcessed = buffer.getLastSequenceNumber();
            buffer.clear();
                        
            // checkpoint once all the records have been consumed
            if (lastSequenceNumberProcessed != null) {
                checkpointer.checkpoint(lastSequenceNumberProcessed);
            }
        } catch (IOException | KinesisClientLibDependencyException | InvalidStateException | ThrottlingException
                | ShutdownException e) {
            LOG.error(e);
            emitter.fail(unprocessed);
        }
    }

    @Override
    public void shutdown(ShutdownInput shutdownInput) {
    	ShutdownReason reason = shutdownInput.getShutdownReason();
    	IRecordProcessorCheckpointer checkpointer = shutdownInput.getCheckpointer();
        LOG.info("Shutting down record processor with shardId: " + shardId + " with reason " + reason);
        if (isShutdown) {
            LOG.warn("Record processor for shardId: " + shardId + " has been shutdown multiple times.");
            return;
        }
        switch (reason) {
            case TERMINATE:
                emit(checkpointer, transformToOutput(buffer.getRecords()));
                try {
                    checkpointer.checkpoint();
                } catch (KinesisClientLibDependencyException | InvalidStateException | ThrottlingException | ShutdownException e) {
                    LOG.error(e);
                }
                break;
            case ZOMBIE:
                break;
            default:
                throw new IllegalStateException("invalid shutdown reason");
        }
        emitter.shutdown();
        isShutdown = true;
    }

}