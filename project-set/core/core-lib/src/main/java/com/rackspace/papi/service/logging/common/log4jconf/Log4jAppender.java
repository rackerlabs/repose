package com.rackspace.papi.service.logging.common.log4jconf;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author malconis
 */
public class Log4jAppender {
    
    private String appenderName;
    private String logger;
    private String layout;
    private String conversionPattern;
    private Map<String,String> appender;
    private static final String LOG4J_APP_PREFIX = "log4j.appender.";
    private static final String LOG4J_PREFIX = "org.apache.log4j.";

    public Log4jAppender(String appenderName, String logger, String layout, String conversionPattern) {
        this.appenderName = appenderName;
        this.logger = LOG4J_PREFIX+logger;
        this.layout = LOG4J_PREFIX+layout;
        this.conversionPattern = conversionPattern;
        this.appender = new HashMap<String, String>();
        createAppender();
    }
    
    
    
    private void createAppender(){
        
        StringBuilder appBuilder = new StringBuilder(LOG4J_APP_PREFIX);
        
        appBuilder.append(appenderName);
        
        appender.put(appBuilder.toString(), logger);
        appBuilder.append(".layout");
        appender.put(appBuilder.toString(), layout);
        appBuilder.append(".ConversionPattern").toString();
        appender.put(appBuilder.toString(),conversionPattern);
        
    }

    public Map<String, String> getAppender() {
        return appender;
    }

    public String getAppenderName() {
        return appenderName;
    }

    public String getConversionPattern() {
        return conversionPattern;
    }

    public String getLayout() {
        return layout;
    }

    public String getLogger() {
        return logger;
    }

    public void setAppenderName(String appenderName) {
        this.appenderName = appenderName;
        createAppender();
    }

    public void setConversionPattern(String conversionPattern) {
        this.conversionPattern = conversionPattern;
        createAppender();
    }

    public void setLayout(String layout) {
        this.layout = layout;
        createAppender();
    }

    public void setLogger(String logger) {
        this.logger = logger;
        createAppender();
    }
    
    public void addProp(String property, String value){
        StringBuilder appBuilder = new StringBuilder(LOG4J_APP_PREFIX);
        appender.put(appBuilder.append(appenderName).append(".").append(property).toString(), value);
    }
    
    
    
    
}
