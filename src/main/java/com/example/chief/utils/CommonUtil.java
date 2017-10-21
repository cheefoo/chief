package com.example.chief.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.codec.digest.DigestUtils;

public class CommonUtil {

	public static String toEsIndexDateFormat(String fromDateTimeString) throws ParseException{
    	SimpleDateFormat fromFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    	SimpleDateFormat toFormat = new SimpleDateFormat("yyyyMM");
    	Date fromDate = fromFormat.parse(fromDateTimeString);

    	return toFormat.format(fromDate).toString();
	}
	
	public static String toEsIdDateFormat(String fromDateTimeString) throws ParseException{
    	SimpleDateFormat fromFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    	SimpleDateFormat toFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    	Date fromDate = fromFormat.parse(fromDateTimeString);

    	return toFormat.format(fromDate).toString();
	}
	
	public static String toS3PrefixDateFormat(String fromDateTimeString) throws ParseException{
    	SimpleDateFormat fromFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    	SimpleDateFormat toFormat = new SimpleDateFormat("yyyy/MM/dd/HH/");
    	Date fromDate = fromFormat.parse(fromDateTimeString);

    	return toFormat.format(fromDate).toString();
	}
	
	public static String generateOrderId(String fromDateTimeString, String orderJson) throws ParseException{
    	SimpleDateFormat fromFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    	SimpleDateFormat toFormat = new SimpleDateFormat("yyyyMMdd");
    	Date fromDate = fromFormat.parse(fromDateTimeString);

    	String orderId = toFormat.format(fromDate).toString() + "-" + DigestUtils.sha256Hex(orderJson);
    	return orderId;
	}
	
}
