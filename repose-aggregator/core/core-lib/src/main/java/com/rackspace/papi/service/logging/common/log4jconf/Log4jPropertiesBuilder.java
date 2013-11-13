package com.rackspace.papi.service.logging.common.log4jconf;

import org.apache.log4j.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
/**
 *
 * @author malconis
 */
public class Log4jPropertiesBuilder {
    
    //bunch of default values
    private Properties properties;
    private Level logLevel;
    private List<Log4jAppender> appenders;
    private static final String ROOT_LOGGER = "log4j.rootLogger";
    
    public Log4jPropertiesBuilder() {
        this.properties = new Properties();
        this.appenders = new ArrayList<Log4jAppender>();
        this.logLevel = Level.DEBUG;
        this.properties.put(ROOT_LOGGER, logLevel.toString());
    }
    
    public Log4jPropertiesBuilder(Level level){
        this.properties = new Properties();
        this.appenders = new ArrayList<Log4jAppender>();
        this.logLevel = level;
        this.properties.put(ROOT_LOGGER, logLevel.toString());
    }
    
    public Properties getLoggingConfig(){
        
        return properties;
    }
    
    public void addLog4jAppender(Log4jAppender log4jAppender){
        StringBuilder rootLogger = new StringBuilder(properties.getProperty(ROOT_LOGGER));
        
        appenders.add(log4jAppender);
        properties.put(ROOT_LOGGER, rootLogger.append(",").append(log4jAppender.getAppenderName()).toString());
        properties.putAll(log4jAppender.getAppender());
    }
    
    public void setProperties(Map<String,String> prop){
        
        properties.clear();
        properties.putAll(prop);
    }
    
    public void setLogLevel(Level level){
        this.logLevel = level;
        
        StringBuilder rootLogger = new StringBuilder(this.logLevel.toString());
        
        for(Log4jAppender app: appenders){
            rootLogger.append(",").append(app.getAppenderName().toString());
        }
        properties.put(ROOT_LOGGER, rootLogger.toString());
    }
    
    public Level getLogLevel(){
        
        return logLevel;
    }
            

}
