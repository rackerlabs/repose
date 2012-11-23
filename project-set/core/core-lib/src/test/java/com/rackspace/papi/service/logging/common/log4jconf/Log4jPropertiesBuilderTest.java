package com.rackspace.papi.service.logging.common.log4jconf;

import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author malconis
 */
public class Log4jPropertiesBuilderTest {

    private static Log4jAppender defaultLog4j = new Log4jAppender("consoleOut", "ConsoleAppender", "PatternLayout", "%-4r [%t] %-5p %c %x - %m%n");
    private static Log4jAppender otherLogger = new Log4jAppender("defaultFile", "RollingFileAppender", "PatternLayout", "%d %-4r [%t] %-5p %c %x - %m%n");
    Log4jPropertiesBuilder log4jPropertiesBuilder  = new Log4jPropertiesBuilder();
    @Before
    public void setUp() {
        
        log4jPropertiesBuilder.addLog4jAppender(defaultLog4j);
        
    }
    
    @Test
    public void shouldHaveOneLoggingConfig(){
        
        String[] rootLogger= log4jPropertiesBuilder.getLoggingConfig().getProperty("log4j.rootLogger").split(",");
        assertTrue(rootLogger[0].equals("DEBUG"));
        assertEquals(2, rootLogger.length);
    }

    @Test
    public void shouldHaveNewLogger(){
        
        log4jPropertiesBuilder.addLog4jAppender(otherLogger);
        String[] rootLogger= log4jPropertiesBuilder.getLoggingConfig().getProperty("log4j.rootLogger").split(",");
        assertEquals(3, rootLogger.length);
    }
    
    @Test
    public void shouldChangeLogLevel(){
        log4jPropertiesBuilder.setLogLevel(Level.WARN);
        String[] rootLogger= log4jPropertiesBuilder.getLoggingConfig().getProperty("log4j.rootLogger").split(",");
        assertTrue(rootLogger[0].equals("WARN"));
    }
    
}
