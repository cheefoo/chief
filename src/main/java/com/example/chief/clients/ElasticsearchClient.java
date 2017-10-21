package com.example.chief.clients;

import java.io.IOException;
import java.util.Collections;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

public class ElasticsearchClient {

	private String host;
	private Integer port;
	
	public ElasticsearchClient(String host,Integer port){
		this.host = host; //"search-domainname-abcdefg.us-east-1.es.amazonaws.com"
		this.port = port;
	}
	
	public void doBulk(String bulkRequest) throws IOException{
		
        RestClient client = RestClient.builder(new HttpHost(this.host, this.port, "https")).build();

        HttpEntity entity = new NStringEntity(bulkRequest, ContentType.APPLICATION_JSON);

        Response response = client.performRequest("POST", "/_bulk", Collections.emptyMap(), entity);

        System.out.println(response.toString());
		
	}
	
}
