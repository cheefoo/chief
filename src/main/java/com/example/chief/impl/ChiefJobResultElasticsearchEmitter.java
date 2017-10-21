package com.example.chief.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.kinesis.connectors.UnmodifiableBuffer;
import com.example.chief.clients.ElasticsearchClient;
import com.example.chief.configuration.ChiefConfiguration;
import com.example.chief.interfaces.IChiefEmitter;
import com.example.chief.model.JobResult;
import com.example.chief.utils.CommonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChiefJobResultElasticsearchEmitter implements IChiefEmitter<byte[]>  {
    private static final Log LOG = LogFactory.getLog(ChiefOrderElasticsearchS3Emitter.class);
    protected String regionName;
    
    protected String esEndpoint;
    protected Integer esPort;
    protected String esIndexPrefix;
    protected ElasticsearchClient elasticsearchClient;
    
	private static final String indexFormat = "{ \"index\" : { \"_index\" : \"%s\", \"_type\" : \"%s\", \"_id\" : \"%s\" } }";
	
    public ChiefJobResultElasticsearchEmitter(ChiefConfiguration configuration) {
        regionName = configuration.REGION_NAME;

        esEndpoint = configuration.ELASTICSEARCH_ENDPOINT;
        esPort = configuration.ELASTICSEARCH_PORT;
        esIndexPrefix = configuration.ELASTICSEARCH_INDEX_PREFIX;
        elasticsearchClient = new ElasticsearchClient(esEndpoint, esPort);
    }

    @Override
    public List<byte[]> emit(final UnmodifiableBuffer<byte[]> buffer, String shardId) throws IOException {
        List<byte[]> records = buffer.getRecords();
        List<byte[]> dedupedRecords = records.stream().distinct().collect(Collectors.toList());
        
        StringBuilder sb = new StringBuilder();

        ObjectMapper mapper = new ObjectMapper();
        JobResult jobResult = null;
        String index = "";
        String jobResultJson = "";
        String yyyymm = "";
        String esDataType = "JobResult";
        
        for (byte[] record : dedupedRecords) {
            try {
            	jobResultJson = new String(record);
                jobResult = mapper.readValue(jobResultJson, JobResult.class);
                yyyymm = CommonUtil.toEsIndexDateFormat(jobResult.getTimestamp());

            	index = String.format(indexFormat, 
            			esIndexPrefix + "-" + yyyymm, 
            			esDataType, 
            			jobResult.getOrderId());
            	
                //sb is a BulkRequest to write in Elasticsearch.
                sb.append(index).append("\n").append(jobResultJson).append("\n");

                
            } catch (Exception e) {
                LOG.error("Error writing record to output stream. Failing this emit attempt. Record: "
                        + Arrays.toString(record),
                        e);
                return buffer.getRecords();
            }
        }
        
        try {
            
            elasticsearchClient.doBulk(sb.toString());
            LOG.info("Successfully emitted " + buffer.getRecords().size() + " records to Elasticsearch in ");
            
            return Collections.emptyList();
            
        } catch (Exception e) {
            LOG.error("Caught exception when uploading file " + esEndpoint + "to Amazon Elasticsearch Service. Failing this emit attempt.", e);
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
        
    }
}
