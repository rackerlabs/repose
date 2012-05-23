package com.rackspace.papi.commons.config.parser.inputstream;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.config.resource.ResourceResolutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class InputStreamConfigurationParserTest {

   public static class WhenReadingResource {
      private InputStreamConfigurationParser instance;
      private ConfigurationResource cr;
      private InputStream stream;
      private ConfigurationResource badCr;

      @Before
      public void setUp() throws IOException {
         instance = new InputStreamConfigurationParser();
         
         // good case
         cr = mock(ConfigurationResource.class);
         stream = mock(InputStream.class);
         when(cr.newInputStream()).thenReturn(stream);
         
         // bad case
         badCr = mock(ConfigurationResource.class);
         when(badCr.newInputStream()).thenThrow(new IOException());
      }

      @Test
      public void shouldGetInputStream() throws IOException {
         InputStream actual = instance.read(cr);
         verify(cr, times(1)).newInputStream();
         assertTrue("Should get input stream", actual == stream);
      }

      @Test(expected=ResourceResolutionException.class)
      public void shouldThrowResourceResolutionException() {
         instance.read(badCr);
      }
   }
}
