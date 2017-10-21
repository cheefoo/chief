package com.example.chief.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.kinesis.connectors.UnmodifiableBuffer;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.example.chief.configuration.ChiefConfiguration;
import com.example.chief.interfaces.IChiefEmitter;
import com.example.chief.model.Order;
import com.example.chief.utils.CommonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChiefOrderFactoryS3Emitter implements IChiefEmitter<byte[]>  {
    private static final Log LOG = LogFactory.getLog(ChiefOrderFactoryS3Emitter.class);
    protected String regionName;
    protected String s3Bucket;
    protected String s3Prefix;
    protected String s3Endpoint;
    protected AmazonS3 s3client;
        
    public ChiefOrderFactoryS3Emitter(ChiefConfiguration configuration) {
        regionName = configuration.REGION_NAME;
    	s3Bucket = configuration.S3_BUCKET;
        s3Endpoint = configuration.S3_ENDPOINT;
        s3Prefix = configuration.S3_PREFIX;
        EndpointConfiguration endpointConfiguration = new EndpointConfiguration(s3Endpoint,regionName);
        s3client = AmazonS3ClientBuilder.standard()
        		.withCredentials(configuration.AWS_CREDENTIALS_PROVIDER)
        		.withEndpointConfiguration(endpointConfiguration)
        		.build(); 
    }

    protected String getS3FileName(String shardId, String s3DatePrefix, String firstSeq) {
    	// shardId +  firstSeq become objectname. 
    	// Even if the worker fails, even if continue from the seq number of the last checkpoint, the object name is always the same.
        return s3Prefix + "/" + s3DatePrefix + shardId + "-" + firstSeq;
    }

    protected String getS3URI(String s3FileName) {
        return "s3://" + s3Bucket + "/" + s3FileName;
    }

    @Override
    public List<byte[]> emit(final UnmodifiableBuffer<byte[]> buffer, String shardId) throws IOException {
        List<byte[]> records = buffer.getRecords();
        List<byte[]> dedupedRecords = records.stream().distinct().collect(Collectors.toList());
        
        ObjectMapper mapper = new ObjectMapper();
        
        // Write all of the records to a compressed output stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] newline = "\n".getBytes();
        
        String orderJson = "";
        String orderId = "";
        String output = "";
        String firstSequenceNumberActivityTimeStamp = "";
        Integer i = 0;
       
        for (byte[] record : dedupedRecords) {
            try {
            	orderJson = new String(record);
                Order order = mapper.readValue(orderJson, Order.class);

                if(i ==0 ){firstSequenceNumberActivityTimeStamp = order.getActivityTimestamp();}
                
                orderId = CommonUtil.generateOrderId(order.getActivityTimestamp(), orderJson);
                output =  orderId + "\t" + orderJson;
            	// baos is a ByteArrayOutputStream to write in S3.
                baos.write(output.getBytes(StandardCharsets.UTF_8));
                baos.write(newline);
                            
                ++i;
            } catch (Exception e) {
                LOG.error("Error writing record to output stream. Failing this emit attempt. Record: "
                        + Arrays.toString(record),
                        e);
                return buffer.getRecords();
            }
        }
        
        String s3URI = "";
        try {
            // Get the Amazon S3 filename
            String s3DatePrefix = CommonUtil.toS3PrefixDateFormat(firstSequenceNumberActivityTimeStamp);
            String s3FileName = getS3FileName(shardId, s3DatePrefix, buffer.getFirstSequenceNumber());
            s3URI = getS3URI(s3FileName);
            
        	ByteArrayInputStream object = new ByteArrayInputStream(baos.toByteArray());
            LOG.debug("Starting upload of file " + s3URI + " to Amazon S3 containing " + dedupedRecords.size() + " records.");
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(baos.size());
            s3client.putObject(s3Bucket, s3FileName, object, meta);
            LOG.info("Successfully emitted " + buffer.getRecords().size() + " records to Amazon S3 in " + s3URI);
                        
            return Collections.emptyList();
            
        } catch (Exception e) {
            LOG.error("Caught exception when uploading file " + s3URI + "to Amazon S3. Failing this emit attempt.", e);
            return buffer.getRecords();
        }
    }

    @Override
    public void fail(List<byte[]> records) {
        for (byte[] record : records) {
            LOG.error("Record failed: " + Arrays.toString(record));
        }
    }

    @Override
    public void shutdown() {
        s3client.shutdown();
    }
}