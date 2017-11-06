package com.example.chief.producer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.example.chief.configuration.ChiefProducerConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChiefJobResultsProducerExecutor {

    // Class variables
	private static ChiefProducerConfiguration config;
    private static Properties properties;
    
    private static final Log LOG = LogFactory.getLog(ChiefOrderProducerExecutor.class);
    
    private static final String PROPERTY_FILE = "JobResultsProducer.properties";
    
    private static final Logger ROOT_LOGGER = Logger.getLogger("");
    private static final Logger PRODUCER_LOGGER =
            Logger.getLogger("com.example.chief.producer");

    /**
     * Sets the global log level to WARNING and the log level for this package to INFO,
     * so that we only see INFO messages for this processor. This is just for the purpose
     * of this tutorial, and should not be considered as best practice.
     *
     */
    private static void setLogLevels() {
        
        ROOT_LOGGER.setLevel(Level.ALL);
        PRODUCER_LOGGER.setLevel(Level.INFO);
    }
    
    public static void main(String[] args) throws Exception {
    	
    	initializeConfig();
  	
    	String streamName = config.KINESIS_OUTPUT_STREAM;
        Region region = RegionUtils.getRegion(config.REGION_NAME);
        if (region == null) {
            System.err.println(config.REGION_NAME + " is not a valid AWS region.");
            System.exit(1);
        }
        
        setLogLevels();
        
        final BlockingQueue<Object> events = new ArrayBlockingQueue<Object>(65536);
        final ExecutorService exec = Executors.newCachedThreadPool();

        // Change this line to use a different implementation
        final AbstractProducerWorker worker = new ChiefProducerWorker(streamName, region.getName(), events);
        exec.submit(worker);

        final LocalDateTime testStart = LocalDateTime.now();
        final LocalDateTime testEnd = testStart.plus(Duration.ofSeconds(config.PRODUCER_DURATION));
        
        ObjectMapper mapper = new ObjectMapper();
        
        final String originalDataFolder = config.ORIGINAL_DATA_FOLDER;
        
        String backupDir = "backup";     
        String backupDirPath = Paths.get(originalDataFolder, backupDir).toString();
		File destFolder = new File(backupDirPath);
		destFolder.mkdir();
		
        exec.submit(() -> {
            try {
                while (LocalDateTime.now().isBefore(testEnd)) {
                	
                	List<File> jsonFiles = readFolder(new File(originalDataFolder));
                	for (File file : jsonFiles) {
                		List<Object> orders = mapper.readValue(file, mapper.getTypeFactory().constructCollectionType(List.class, Object.class));
                		
                		for (Object order : orders) {
                			//put each json data to queue
                			//System.out.println(mapper.writeValueAsString(order));
                			events.put(mapper.writeValueAsString(order));
						}
                		
                		LOG.info("Emitted" + Integer.toString(orders.size()) + "records to KPL.");
                		
                		Path backupFilePath = Paths.get(backupDirPath, file.getName());
                		File dest = new File(backupFilePath.toString());
                		if(dest.exists()){
                			dest.delete();
                		}
                		file.renameTo(dest);
					}
                	
                    Thread.sleep(10000);
                }

                worker.stop();
                exec.shutdown();

            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        });

        // This reports the average records per second over a 10 second sliding window
        new Thread(() -> {
            Map<Long, Long> history = new TreeMap<>();
            try {
                while (!exec.isTerminated()) {
                    long seconds = Duration.between(testStart, LocalDateTime.now()).getSeconds();
                    long records = worker.recordsPut();
                    history.put(seconds, records);

                    long windowStart = seconds - 10;
                    long recordsAtWinStart =
                            history.containsKey(windowStart) ? history.get(windowStart) : 0;
                    double rps = (double)(records - recordsAtWinStart) / 10;

                    System.out.println(String.format(
                            "%d seconds, %d records total, %.2f RPS (avg last 10s)",
                            seconds,
                            records,
                            rps));
                    Thread.sleep(1000);
                }
                System.out.println("Finished.");
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }).start();
    }
    
    private static void initializeConfig(){
        String configFile = PROPERTY_FILE;
    	InputStream configStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFile);
        
        if (configStream == null) {
            String msg = "Could not find resource " + configFile + " in the classpath";
            throw new IllegalStateException(msg);
        }
        
        properties = new Properties();
        
        try {
            properties.load(configStream);
            configStream.close();
        } catch (IOException e) {
            String msg = "Could not load properties file " + configFile + " from classpath";
            throw new IllegalStateException(msg, e);
        }
        
        config = new ChiefProducerConfiguration(properties, new DefaultAWSCredentialsProviderChain());

    }
    
    public static List<File> readFolder( File dir ) {
    	List<File> jsonFiles = new ArrayList<File>();
    	
        File[] files = dir.listFiles();
        if( files == null )
          return jsonFiles;
        for( File file : files ) {
          if( !file.exists() )
            continue;
          //else if( file.isDirectory() )
          //  readFolder( file );
          else if( file.isFile() )
        	  jsonFiles.add(file);
        }
        
        return jsonFiles;
      }

}