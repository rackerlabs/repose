package com.rackspace.papi.service.logging.common.log4jconf;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author malconis
 */
public class Log4jAppenderTest {

    public Log4jAppenderTest() {
    }
    private Log4jAppender defaultLog4j;
    String name = "consoleOut";
    String appender = "ConsoleAppender";
    String layout = "PatternLayout";
    String pattern = "%-4r [%t] %-5p %c %x - %m%n";
    String LOG4J_PREFIX = "org.apache.log4j.";
    @Before
    public void setUp() {

        defaultLog4j = new Log4jAppender(name, appender, layout, pattern);

    }

    @Test
    public void shouldDisplayDefaultValues() {

        assertEquals(name, defaultLog4j.getAppenderName());
        assertEquals(LOG4J_PREFIX+appender, defaultLog4j.getLogger());
        assertEquals(LOG4J_PREFIX+layout, defaultLog4j.getLayout());
        assertEquals(pattern, defaultLog4j.getConversionPattern());
    }
    
    @Test
    public void shouldContainProperKeyNames(){
        
        assertTrue(defaultLog4j.getAppender().containsKey("log4j.appender."+name));
        assertTrue(defaultLog4j.getAppender().containsKey("log4j.appender."+name+"."+"layout"));
        assertTrue(defaultLog4j.getAppender().containsKey("log4j.appender."+name+"."+"layout."+"ConversionPattern"));
    }
    
    @Test
    public void shouldAddNewProperty(){
        String propertyName = "MaxFileSize";
        String propertyValue = "2MB";
        
        defaultLog4j.addProp(propertyName, propertyValue);
        
        assertTrue(defaultLog4j.getAppender().get("log4j.appender.consoleOut.MaxFileSize").equals(propertyValue));
        
    }
}
