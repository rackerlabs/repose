package org.openrepose.core.filter;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.junit.Test;
import org.openrepose.powerfilter.RequestTracer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * A jUnit test class for the RequestTracer class.
 */
public class RequestTracerTest {

    MutableHttpServletResponse response;

    @Test
    public void shouldReturnTimeSinceInitialization() {
        boolean trace = true;
        boolean addHeader = true;

        long time;
        RequestTracer rt;

        rt = new RequestTracer(trace, addHeader);
        try {
            Thread.sleep(1000L);
        } catch (Exception e) {}
        time = rt.traceEnter();

        assertFalse("The returned time should not be 0", time == 0);

        trace = true;
        addHeader = false;

        rt = new RequestTracer(trace, addHeader);
        try {
            Thread.sleep(1000L);
        } catch (Exception e) {}
        time = rt.traceEnter();

        assertFalse("The returned time should not be 0", time == 0);
    }

    @Test
    public void shouldNotReturnTimeSinceInitialization() {
        boolean trace = false;
        boolean addHeader = true;

        long time;
        RequestTracer rt;

        rt = new RequestTracer(trace, addHeader);
        try {
            Thread.sleep(1000L);
        } catch (Exception e) {}
        time = rt.traceEnter();

        assertTrue("The returned time should be 0", time == 0);

        trace = false;
        addHeader = false;

        rt = new RequestTracer(trace, addHeader);
        try {
            Thread.sleep(1000L);
        } catch (Exception e) {}
        time = rt.traceEnter();

        assertTrue("The returned time should be 0", time == 0);
    }

    @Test
    public void shouldReturnTimeSinceEnterCallAndAddHeaderToResponse() {
        boolean trace = true;
        boolean addHeader = true;

        long time;
        RequestTracer rt;

        response = mock(MutableHttpServletResponse.class);

        rt = new RequestTracer(trace, addHeader);
        rt.traceEnter();
        try {
            Thread.sleep(1000L);
        } catch (Exception e) {}
        time = rt.traceExit(response, "myFilter");

        assertFalse("The returned time should not be 0", time == 0);
        // The x-trace-request header should have been added to the response
        verify(response, times(1)).addHeader(eq("X-myFilter-Time"), anyString());
    }

    @Test
    public void shouldReturnTimeSinceEnterCallAndShouldNotAddHeaderToResponse() {
        boolean trace = true;
        boolean addHeader = false;

        long time;
        RequestTracer rt;

        response = mock(MutableHttpServletResponse.class);

        rt = new RequestTracer(trace, addHeader);
        rt.traceEnter();
        try {
            Thread.sleep(1000L);
        } catch (Exception e) {}
        time = rt.traceExit(response, "myFilter");

        assertFalse("The returned time should not be 0", time == 0);
        // The x-trace-request header should not have been added to the response
        verify(response, never()).addHeader(eq("X-myFilter-Time"), anyString());
    }

    @Test
    public void shouldNotReturnTimeSinceEnterCallAndShouldNotAddHeaderToResponse() {
        boolean trace = false;
        boolean addHeader = true;

        long time;
        RequestTracer rt;

        response = mock(MutableHttpServletResponse.class);

        rt = new RequestTracer(trace, addHeader);
        rt.traceEnter();
        try {
            Thread.sleep(1000L);
        } catch (Exception e) {}
        time = rt.traceExit(response, "myFilter");

        assertTrue("The returned time should be 0", time == 0);
        // The x-trace-request header should not have been added to the response
        verify(response, never()).addHeader(eq("X-myFilter-Time"), anyString());

        trace = false;
        addHeader = false;

        response = mock(MutableHttpServletResponse.class);

        rt = new RequestTracer(trace, addHeader);
        rt.traceEnter();
        try {
            Thread.sleep(1000L);
        } catch (Exception e) {}
        time = rt.traceExit(response, "myFilter");

        assertTrue("The returned time should be 0", time == 0);
        // The x-trace-request header should not have been added to the response
        verify(response, never()).addHeader(eq("X-myFilter-Time"), anyString());
    }
}
