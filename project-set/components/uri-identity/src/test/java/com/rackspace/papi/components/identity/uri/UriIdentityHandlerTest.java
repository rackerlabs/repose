package com.rackspace.papi.components.identity.uri;



import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterDirector;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class UriIdentityHandlerTest {

    public static class WhenHandlingRequests {

        private List<Pattern> patterns;
        private static String GROUP = "DEFAULT_GROUP";

        private static Double QUALITY = 0.5;
        private static String QUALITY_VALUE = ";q=0.5";
        private static String URI1 = "/someuri/1234/morestuff";
        private static String REGEX1 = ".*/[^\\d]*/(\\d*)/.*";
        private static String USER1 = "1234";
        private static String URI2 = "/someuri/abc/someuser";
        private static String REGEX2 = ".*/[^\\d]*/abc/(.*)";
        private static String USER2 = "someuser";
        private static String URIFAIL = "/nouserinformation";
        private HttpServletRequest request;
        private ReadableHttpServletResponse response;
        private UriIdentityHandler handler;

        @Before
        public void setUp() {

            patterns = new ArrayList<Pattern>();
            patterns.add(Pattern.compile(REGEX1));
            patterns.add(Pattern.compile(REGEX2));

            handler = new UriIdentityHandler(patterns, GROUP, QUALITY);
            request = mock(HttpServletRequest.class);
            response = mock(ReadableHttpServletResponse.class);

        }

        @Test
        public void shouldSetTheUserHeaderToTheRegexResult() {
            when(request.getRequestURI()).thenReturn(URI1);

            FilterDirector result = handler.handleRequest(request, response);

            Set<String> values = result.requestHeaderManager().headersToAdd().get(PowerApiHeader.USER.toString().toLowerCase());
            assertFalse("Should have " + PowerApiHeader.USER.toString() + " header set.", values == null || values.isEmpty());

            String userName = values.iterator().next();

            assertEquals("Should find user name in header", USER1 + QUALITY_VALUE, userName);
        }

        @Test
        public void shouldSetTheUserHeaderToThe2ndRegexResult() {
            when(request.getRequestURI()).thenReturn(URI2);

            FilterDirector result = handler.handleRequest(request, response);

            Set<String> values = result.requestHeaderManager().headersToAdd().get(PowerApiHeader.USER.toString().toLowerCase());
            assertFalse("Should have " + PowerApiHeader.USER.toString() + " header set.", values == null || values.isEmpty());

            String userName = values.iterator().next();

            assertEquals("Should find user name in header", USER2 + QUALITY_VALUE, userName);
        }

        @Test
        public void shouldNotHaveUserHeader() {
            when(request.getRequestURI()).thenReturn(URIFAIL);

            FilterDirector result = handler.handleRequest(request, response);

            Set<String> values = result.requestHeaderManager().headersToAdd().get(PowerApiHeader.USER.toString().toLowerCase());
            assertTrue("Should not have " + PowerApiHeader.USER.toString() + " header set.", values == null || values.isEmpty());

        }
    }
}