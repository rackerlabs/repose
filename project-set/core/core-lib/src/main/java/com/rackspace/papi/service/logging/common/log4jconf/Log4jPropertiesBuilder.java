/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.service.logging.common.log4jconf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
/**
 *
 * @author malconis
 */
public class Log4jPropertiesBuilder {
    
    //bunch of default values
    private Properties properties;
    private String logLevel = "DEBUG";
    private ArrayList<Log4jAppender> appenders;
    private final String ROOT_LOGGER = "log4j.rootLogger";
    
    public Log4jPropertiesBuilder() {
        this.properties = new Properties();
        this.appenders = new ArrayList<Log4jAppender>();
        this.properties.put(ROOT_LOGGER, logLevel);
    }
    
    public Properties getLoggingConfig(){
        
        return properties;
    }
    
    public void addLog4jAppender(Log4jAppender log4jAppender){
        StringBuilder rootLogger = new StringBuilder(properties.getProperty(ROOT_LOGGER));
        
        appenders.add(log4jAppender);
        properties.put("log4j.rootLogger", rootLogger.append(",").append(log4jAppender.getAppenderName()).toString());
        properties.putAll(log4jAppender.getAppender());
    }
    
    public void setProperties(Map<String,String> prop){
        
        properties.clear();
        properties.putAll(prop);
    }
    
    public void setLogLevel(String level){
        this.logLevel = level;
        
        StringBuilder rootLogger = new StringBuilder(this.logLevel);
        
        for(Log4jAppender app: appenders){
            rootLogger.append(",").append(app.getAppenderName().toString());
        }
        properties.put(ROOT_LOGGER, rootLogger.toString());
    }

}
