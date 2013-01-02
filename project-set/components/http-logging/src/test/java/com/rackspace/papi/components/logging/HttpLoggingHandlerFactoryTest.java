package com.rackspace.papi.components.logging;

import com.rackspace.papi.components.logging.config.FileTarget;
import com.rackspace.papi.components.logging.config.HttpLog;
import com.rackspace.papi.components.logging.config.HttpLoggingConfig;
import com.rackspace.papi.components.logging.config.Targets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class HttpLoggingHandlerFactoryTest {

   public static class WhenGettingListeners {
      private HttpLoggingHandlerFactory instance;
      @Before
      public void setUp() {
         instance = new HttpLoggingHandlerFactory();
      }

      @Test
      public void shouldHaveConfigListener() {
         int expected = 1;
         assertEquals("Should have a config listener", expected, instance.getListeners().size());
      }
      
   }

   public static class WhenBuildingHandlers {
      private HttpLoggingHandlerFactory instance;
      private HttpLoggingConfig loggingConfig;
      private HttpLoggingConfig nullFileTargetLoggerConfig;
      private int loggerCount = 10;
      private File file;
      
      @Before
      public void setUp() throws IOException {
         instance = new HttpLoggingHandlerFactory();
         loggingConfig = new HttpLoggingConfig();
         
         file = File.createTempFile("FileLoggerTest01", "tfile");
         
         for (int i = 0; i < loggerCount; i++) {
            Targets targets = new Targets();
            FileTarget target = new FileTarget();
            target.setLocation(file.getAbsolutePath());
            targets.getFile().add(target);

            HttpLog log = new HttpLog();
            log.setFormat("format" + i);
            log.setId("log" + i);
            log.setTargets(targets);
            
            loggingConfig.getHttpLog().add(log);
         }
         
         nullFileTargetLoggerConfig = new HttpLoggingConfig();
         Targets targets = new Targets();
//         targets.getFile() =null;

         int id = loggerCount + 1;
         HttpLog log = new HttpLog();
         log.setFormat("format" + id);
         log.setId("log" + id);
         log.setTargets(targets);
         
         nullFileTargetLoggerConfig.getHttpLog().add(log);
      }
      
      @After
      public void after() {
         assertTrue("file should have been deleted", file.delete());
      }

    

      @Test
      public void shouldGetNewLoggersWhenConfigUpdated() {
         instance.configurationUpdated(loggingConfig);
         final int actual = instance.getLoggers().size();

         assertEquals("", loggerCount, actual);
      }
      
        @Test
      public void shouldGetHandler() {
         HttpLoggingHandler result = instance.buildHandler();
        // assertNotNull("Should get a handler", result);
      }
      
      @Test
      public void shouldNotAddLoggersWithNullFiles() {
         instance.configurationUpdated(nullFileTargetLoggerConfig);
         final int actualWrapperCount = instance.getLoggers().size();
         final int expectedWrapperCount = 1;
         
         final int actualLoggerCount = instance.getLoggers().get(0).getLoggers().size();
         final int expectedLoggerCount = 0;

         assertEquals("Should have 1 logger wrapper", expectedWrapperCount, actualWrapperCount);
         assertEquals("Logger Wrapper should have no loggers", expectedLoggerCount, actualLoggerCount);
         
      }
   }

}
