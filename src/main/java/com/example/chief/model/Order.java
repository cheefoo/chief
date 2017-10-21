package com.example.chief.model;

public class Order {

	//{"userid": "hoge@example.com","gender": "M","size": "S","material": "polyester","design": "amazon","activityTimestamp": "2017-10-05 16:58:58"}
	//{"userid": "qhickman@example.com","gender": "O","size": "{"bust": "16.9346039325","weight": "416.872545693","hip": "79.5164396738","waist": "55.8186770696"},"material": denim","design": "custom","activityTimestamp": "2017-10-05 17:03:59"}
	
    private String userid;
    private String gender;
    private Object size;
    private String material;
    private String design;
    private String activityTimestamp;
    
    public Order(){}
    
    public Order(
    		String userid, 
    		String gender, 
    		Object size, 
    		String material, 
    		String design,
    		String activityTimestamp){
    	this.userid = userid;
    	this.gender = gender;
    	this.size = size;
    	this.material = material;
    	this.design = design;
    	this.activityTimestamp = activityTimestamp;
    }
    
    public String getUserid() { return this.userid; }
    public void setUserid(String userid) { this.userid = userid; }

    public String getGender() { return this.gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public Object getSize() { return this.size; }
    public void setSize(Object size) { this.size = size; }
    
    public String getMaterial() { return this.material; }
    public void setMaterial(String material) { this.material = material; }
    
    public String getDesign() { return this.design; }
    public void setDesign(String design) { this.design = design; }

    public String getActivityTimestamp() { return this.activityTimestamp; }
    public void setActivityTimestamp(String activityTimestamp) { this.activityTimestamp = activityTimestamp; }
}
