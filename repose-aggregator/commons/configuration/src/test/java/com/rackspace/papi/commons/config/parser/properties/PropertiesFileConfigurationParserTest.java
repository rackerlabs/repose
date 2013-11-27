package com.rackspace.papi.commons.config.parser.properties;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.config.resource.ResourceResolutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.junit.After;
import org.junit.AfterClass;

import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class PropertiesFileConfigurationParserTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

   public static class WhenReadingPropertiesFile {

      private PropertiesFileConfigurationParser instance;
      private ConfigurationResource cr;
      private ConfigurationResource badCr;
      private Properties props;

      @Before
      public void setUp() throws IOException {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         instance = new PropertiesFileConfigurationParser();

         props = new Properties();
         props.setProperty("key", "value");
         props.setProperty("key2", "some other value");
         props.store(out, "TEST");
         cr = mock(ConfigurationResource.class);
         when(cr.newInputStream()).thenReturn(new ByteArrayInputStream(out.toByteArray()));
         badCr = mock(ConfigurationResource.class);
         when(badCr.newInputStream()).thenThrow(new IOException());
      }

      @Test
      public void shouldReturnValidPropertiesFile() {
         Properties actual = instance.read(cr);
         assertEquals("Should get properties file", props, actual);
      }
      
      @Test(expected=ResourceResolutionException.class)
      public void testRead() {
         instance.read(badCr);
      }
   }

}
