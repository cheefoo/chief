package com.example.chief.model;

public class JobResult {

	//{userId: Ågaaa@Åh, orderId: Åg1234Åh, jobStatus: ÅgcompletedÅh, timestamp: Åg2017-10-05:11:2100ZÅh}
	private String userId;
	private String orderId;
    private String factoryId;
    private String robotId;
    private String jobStatus;
    private String timestamp;
    
    public JobResult(){}
    
    public JobResult(
    		String userId, 
    		String orderId, 
    		String factoryId, 
    		String robotId, 
    		String jobStatus,
    		String timestamp
    		){
    	this.userId = userId;
    	this.orderId = orderId;
    	this.factoryId = factoryId;
    	this.robotId = robotId;
    	this.jobStatus = jobStatus;
    	this.timestamp = timestamp;
    }

    public String getUserId() { return this.userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getOrderId() { return this.orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getFactoryId() { return this.factoryId; }
    public void setFactoryId(String factoryId) { this.factoryId = factoryId; }
    
    public String getRobotId() { return this.robotId; }
    public void setRobotId(String robotId) { this.robotId = robotId; }

    public String getJobStatus() { return this.jobStatus; }
    public void setJobStatus(String jobStatus) { this.jobStatus = jobStatus; }
    
    public String getTimestamp() { return this.timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
       
}
