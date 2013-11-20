package com.rackspace.papi.commons.util.servlet.http;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.mock;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: May 19, 2011
 * Time: 10:09:18 AM
 */
@RunWith(Enclosed.class)
public class HttpServletHelperTest {
    public static class WhenValidatingRequestsAndResponses {
        private HttpServletRequest validRequest;
        private ServletRequest invalidRequest;
        private HttpServletResponse validResponse;
        private ServletResponse invalidResponse;
        private Logger logger;

        @Before
        public void setup() {
            validRequest = mock(HttpServletRequest.class);
            invalidRequest = mock(ServletRequest.class);
            validResponse = mock(HttpServletResponse.class);
            invalidResponse = mock(ServletResponse.class);
            logger = mock(Logger.class);
        }

        @Test
        public void shouldDoNothingIfBothRequestAndResponseAreValid() throws ServletException {
            HttpServletHelper.verifyRequestAndResponse(logger, validRequest, validResponse);
        }

        @Test(expected = ServletException.class)
        public void shouldRaiseExceptionDoNothingIfRequestIsInvalid() throws ServletException {
            HttpServletHelper.verifyRequestAndResponse(logger, invalidRequest, validResponse);
        }

        @Test(expected = ServletException.class)
        public void shouldRaiseExceptionDoNothingIfResponseIsInvalid() throws ServletException {
            HttpServletHelper.verifyRequestAndResponse(logger, validRequest, invalidResponse);
        }

        @Test(expected = ServletException.class)
        public void shouldRaiseExceptionDoNothingIfRequestAndResponseAreInvalid() throws ServletException {
            HttpServletHelper.verifyRequestAndResponse(logger, invalidRequest, invalidResponse);
        }
    }
}
