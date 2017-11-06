package com.example.chief.producer;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.amazonaws.services.kinesis.producer.Metric;
import com.amazonaws.services.kinesis.producer.UserRecordFailedException;
import com.amazonaws.services.kinesis.producer.UserRecordResult;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class ChiefProducerWorker extends AbstractProducerWorker {
    private static final Random RANDOM = new Random();
    private static final Log log = LogFactory.getLog(ChiefProducerWorker.class);
    
    private final KinesisProducer kinesis;

    private String streamName;
    
    public ChiefProducerWorker(String streamName,String region, BlockingQueue<Object> inputQueue) {
        super(inputQueue);
        this.streamName = streamName;

        kinesis = new KinesisProducer(new KinesisProducerConfiguration()
                .setRegion(region)
                .setRecordMaxBufferedTime(5000));
    }

    @Override
    protected void runOnce() throws Exception {
    	Object event = inputQueue.take();
        String partitionKey = UUID.randomUUID().toString();
        String payload =  event.toString();
        
        //System.out.println(payload);
        
        ByteBuffer data = ByteBuffer.wrap(payload.getBytes("UTF-8"));
        while (kinesis.getOutstandingRecordsCount() > 1e4) {
            Thread.sleep(1);
        }
        recordsPut.getAndIncrement();

        // Add user record using KPL
        ListenableFuture<UserRecordResult> f =
                kinesis.addUserRecord(streamName, partitionKey, data);
        Futures.addCallback(f, new FutureCallback<UserRecordResult>() {
            @Override
            public void onSuccess(UserRecordResult result) {
                long totalTime = result.getAttempts().stream()
                        .mapToLong(a -> a.getDelay() + a.getDuration())
                        .sum();
                // Only log with a small probability, otherwise it'll be very
                // spammy
                if (RANDOM.nextDouble() < 1e-5) {
                    log.info(String.format(
                            "Succesfully put record, partitionKey=%s, "
                                    + "payload=%s, sequenceNumber=%s, "
                                    + "shardId=%s, took %d attempts, "
                                    + "totalling %s ms",
                            partitionKey, payload, result.getSequenceNumber(),
                            result.getShardId(), result.getAttempts().size(),
                            totalTime));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof UserRecordFailedException) {
                    UserRecordFailedException e =
                            (UserRecordFailedException) t;
                    UserRecordResult result = e.getResult();

                    String errorList =
                        StringUtils.join(result.getAttempts().stream()
                            .map(a -> String.format(
                                "Delay after prev attempt: %d ms, "
                                        + "Duration: %d ms, Code: %s, "
                                        + "Message: %s",
                                a.getDelay(), a.getDuration(),
                                a.getErrorCode(),
                                a.getErrorMessage()))
                            .collect(Collectors.toList()), "\n");

                    log.error(String.format(
                            "Record failed to put, partitionKey=%s, "
                                    + "payload=%s, attempts:\n%s",
                            partitionKey, payload, errorList));
                }
            };
        });
    }

    
    @Override
    public long recordsPut() {
        try {
            return kinesis.getMetrics("UserRecordsPut").stream()
                .filter(m -> m.getDimensions().size() == 2)
                .findFirst()
                .map(Metric::getSum)
                .orElse(0.0)
                .longValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        super.stop();
        kinesis.flushSync();
        kinesis.destroy();
    }
}