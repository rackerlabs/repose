package com.rackspace.papi.service.proxy.ning;

import com.ning.http.client.AsyncHandler.STATE;
import com.ning.http.client.*;
import com.ning.http.client.Response.ResponseBuilder;
import java.io.IOException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class ResponseHandlerTest {

    public static class WhenReceivingData {
        private ResponseBuilder builder;
        private HttpServletResponse response;
        private HttpResponseBodyPart part;
        private ResponseHandler instance;
        private ServletOutputStream out;

        @Before
        public void setUp() throws IOException {
            builder = mock(Response.ResponseBuilder.class);
            response = mock(HttpServletResponse.class);
            part = mock(HttpResponseBodyPart.class);
            out = mock(ServletOutputStream.class);
            when(response.getOutputStream()).thenReturn(out);
            instance = new ResponseHandler(response, builder);
        }
        
        @Test
        public void shouldWriteBodyPart() throws IOException {
            STATE actual = instance.onBodyPartReceived(part);
            assertEquals(AsyncHandler.STATE.CONTINUE, actual);
            verify(part).writeTo(eq(out));
            verify(response).getOutputStream();
        }

        @Test
        public void shouldAccumulateState() throws IOException {
            HttpResponseStatus status = mock(HttpResponseStatus.class);
            STATE actual = instance.onStatusReceived(status);
            assertEquals(AsyncHandler.STATE.CONTINUE, actual);
            verify(builder).accumulate(eq(status));
        }
        
        @Test
        public void shouldAccumulateHeaders() {
            HttpResponseHeaders headers = mock(HttpResponseHeaders.class);
            STATE actual = instance.onHeadersReceived(headers);
            assertEquals(AsyncHandler.STATE.CONTINUE, actual);
            verify(builder).accumulate(eq(headers));
        }

        @Test
        public void shouldBuildOnCompletion() {
            instance.onCompleted();
            verify(builder).build();
        }
    }

}
