package com.example.chief.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.kinesis.connectors.UnmodifiableBuffer;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.example.chief.clients.ElasticsearchClient;
import com.example.chief.configuration.ChiefConfiguration;
import com.example.chief.interfaces.IChiefEmitter;
import com.example.chief.model.Order;
import com.example.chief.model.Size;
import com.example.chief.utils.CommonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChiefOrderElasticsearchS3Emitter implements IChiefEmitter<byte[]>  {
    private static final Log LOG = LogFactory.getLog(ChiefOrderElasticsearchS3Emitter.class);
    protected String regionName;
    protected String s3Bucket;
    protected String s3Prefix;
    protected String s3Endpoint;
    protected AmazonS3 s3client;
    
    protected String esEndpoint;
    protected Integer esPort;
    protected String esIndexPrefix;
    protected ElasticsearchClient elasticsearchClient;
    
	private static final String indexFormat = "{ \"index\" : { \"_index\" : \"%s\", \"_type\" : \"%s\", \"_id\" : \"%s\" } }";
	private static final String ONLINE_ORDER = "OnlineOrder";
	private static final String STORE_ORDER = "StoreOrder";
	
    public ChiefOrderElasticsearchS3Emitter(ChiefConfiguration configuration) {
        regionName = configuration.REGION_NAME;
    	s3Bucket = configuration.S3_BUCKET;
        s3Endpoint = configuration.S3_ENDPOINT;
        s3Prefix = configuration.S3_PREFIX;
        EndpointConfiguration endpointConfiguration = new EndpointConfiguration(s3Endpoint,regionName);
        s3client = AmazonS3ClientBuilder.standard()
        		.withCredentials(configuration.AWS_CREDENTIALS_PROVIDER)
        		.withEndpointConfiguration(endpointConfiguration)
        		.build(); 

        esEndpoint = configuration.ELASTICSEARCH_ENDPOINT;
        esPort = configuration.ELASTICSEARCH_PORT;
        esIndexPrefix = configuration.ELASTICSEARCH_INDEX_PREFIX;
        elasticsearchClient = new ElasticsearchClient(esEndpoint, esPort);
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
        
        // Write all of the records to a compressed output stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringBuilder sb = new StringBuilder();
        byte[] newline = "\n".getBytes();

        ObjectMapper mapper = new ObjectMapper();
        Order order = null;
        String index = "";
        String orderJson = "";
        String yyyymm = "";
        String esDataType = "";
        String firstSequenceNumberActivityTimeStamp = "";
        Integer i = 0;
        String regex = "bust|weight|hip|waist";
        Pattern p = Pattern.compile(regex);
        
        for (byte[] record : dedupedRecords) {
            try {
            	// baos is a ByteArrayOutputStream to write in S3.
                baos.write(record);
                baos.write(newline);
                
                orderJson = new String(record, StandardCharsets.UTF_8);
                order = mapper.readValue(orderJson, Order.class);
                yyyymm = CommonUtil.toEsIndexDateFormat(order.getActivityTimestamp());
                if(i ==0 ){firstSequenceNumberActivityTimeStamp = order.getActivityTimestamp();}
                
            	// If size is not Size Class, treat as OnlineOrder.
            	if(p.matcher(order.getSize().toString()).find()){
                    esDataType = STORE_ORDER;         		
            	}else{
            		esDataType = ONLINE_ORDER;
                    Size size = new Size();
                    size.setTemplateSize(order.getSize().toString());
                    order.setSize(size);
                    orderJson = mapper.writeValueAsString(order); 
            	}

            	index = String.format(indexFormat, 
            			esIndexPrefix + "-" + yyyymm, 
            			esDataType, 
            			CommonUtil.generateOrderId(order.getActivityTimestamp(), orderJson));
            	
                //sb is a BulkRequest to write in Elasticsearch.
                sb.append(index).append("\n").append(orderJson).append("\n");
                
                ++i;
            } catch (Exception e) {
                LOG.error("Error writing record to output stream. Failing this emit attempt. Record: "
                        + Arrays.toString(record),
                        e);
                return buffer.getRecords();
            }
        }
        
        // Get the Amazon S3 filename
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
            
            elasticsearchClient.doBulk(sb.toString());
            LOG.info("Successfully emitted " + buffer.getRecords().size() + " records to Elasticsearch in ");
            
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
