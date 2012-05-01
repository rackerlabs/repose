package com.rackspace.papi.components.unorm;

import com.rackspace.papi.commons.util.http.normal.Normalizer;
import com.rackspace.papi.commons.util.regex.RegexSelector;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.unorm.normalizer.MediaTypeNormalizer;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class UriNormalizationHandlerTest {

   @Ignore
   public static class TestParent {

      protected UriNormalizationHandler handler;
      protected HttpServletRequest mockedRequest;

      @Before
      public final void beforeAll() {
         mockedRequest = mock(HttpServletRequest.class);

         when(mockedRequest.getRequestURI()).thenReturn("/a/really/nifty/uri");
         when(mockedRequest.getQueryString()).thenReturn("a=1&b=2&c=3&d=4");

         final RegexSelector<Normalizer<String>> selectors = new RegexSelector<Normalizer<String>>();
         final Normalizer<String> mockedNormalizer = mock(Normalizer.class);
         when(mockedNormalizer.normalize(anyString())).thenReturn("a=1");

         selectors.addPattern(".*", mockedNormalizer);

         handler = new UriNormalizationHandler(selectors, mock(MediaTypeNormalizer.class));
      }
   }

   public static class WhenNormalizingRequestURIQueryParameters extends TestParent {

      @Test
      public void shouldFilterParameters() {
         final FilterDirectorImpl director = (FilterDirectorImpl) handler.handleRequest(mockedRequest, mock(ReadableHttpServletResponse.class));

         assertEquals("Director must have a normalized query parameter string set.", "a=1", director.getRequestUriQuery());
      }
   }
}
