package com.example.chief.model;

public class Size {
	//"bust":"22", "weight":"140", "hip":"50", waist:"22"
	
    private String bust;
    private String weight;
    private String hip;
    private String waist;
    private String templateSize;
    
    public Size(){}
    
    public Size(
    		String bust, 
    		String weight, 
    		String hip, 
    		String waist){
    	this.bust = bust;
    	this.weight = weight;
    	this.hip = hip;
    	this.waist = waist;
    }
    
    public Size(String templateSize){
    	this.templateSize = templateSize;
    }
    
    public String getBust() { return this.bust; }
    public void setBust(String bust) { this.bust = bust; }

    public String getWeight() { return this.weight; }
    public void setWeight(String weight) { this.weight = weight; }
    
    public String getHip() { return this.hip; }
    public void setHip(String hip) { this.hip = hip; }

    public String getWaist() { return this.waist; }
    public void setWaist(String waist) { this.waist = waist; }
       
    public String getTemplateSize() { return this.templateSize; }
    public void setTemplateSize(String templateSize) { this.templateSize = templateSize; }
}
