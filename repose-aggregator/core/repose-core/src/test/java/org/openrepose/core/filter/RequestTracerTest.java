/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.powerfilter.RequestTracer;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * A jUnit test class for the RequestTracer class.
 */
public class RequestTracerTest {

    final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
    final ListAppender listAppender = (ListAppender) loggerContext.getConfiguration().getAppender("List0");

    HttpServletResponse response;

    @Before
    public void setUp() {
        listAppender.clear();
    }

    @Test
    public void shouldReturnTimeSinceInitialization() throws Exception {
        boolean trace = true;
        boolean addHeader = true;

        long time;
        RequestTracer rt;

        rt = new RequestTracer(trace, addHeader);
        Thread.sleep(1000L);
        time = rt.traceEnter();

        assertThat("The returned time should not be 0", time, not(equalTo(0)));

        trace = true;
        addHeader = false;

        rt = new RequestTracer(trace, addHeader);
        Thread.sleep(1000L);
        time = rt.traceEnter();

        assertThat("The returned time should not be 0", time, not(equalTo(0)));
    }

    @Test
    public void shouldNotReturnTimeSinceInitialization() throws Exception {
        boolean trace = false;
        boolean addHeader = true;

        long time;
        RequestTracer rt;

        rt = new RequestTracer(trace, addHeader);
        Thread.sleep(1000L);
        time = rt.traceEnter();

        assertThat("The returned time should be 0", time, equalTo(0L));

        trace = false;
        addHeader = false;

        rt = new RequestTracer(trace, addHeader);
        Thread.sleep(1000L);
        time = rt.traceEnter();

        assertThat("The returned time should be 0", time, equalTo(0L));
    }

    @Test
    public void shouldReturnTimeSinceEnterCallAndAddHeaderToResponse() throws Exception {
        boolean trace = true;
        boolean addHeader = true;

        long time;
        RequestTracer rt;

        response = mock(HttpServletResponse.class);

        rt = new RequestTracer(trace, addHeader);
        rt.traceEnter();
        Thread.sleep(1000L);
        time = rt.traceExit(response, "myFilter");

        assertThat("The returned time should not be 0", time, not(equalTo(0)));
        // The x-trace-request header should have been added to the response
        verify(response, times(1)).addHeader(eq("X-myFilter-Time"), anyString());
    }

    @Test
    public void shouldReturnTimeSinceEnterCallAndShouldNotAddHeaderToResponse() throws Exception {
        boolean trace = true;
        boolean addHeader = false;

        long time;
        RequestTracer rt;

        response = mock(HttpServletResponse.class);

        rt = new RequestTracer(trace, addHeader);
        rt.traceEnter();
        Thread.sleep(1000L);
        time = rt.traceExit(response, "myFilter");

        assertThat("The returned time should not be 0", time, not(equalTo(0)));
        // The x-trace-request header should not have been added to the response
        verify(response, never()).addHeader(eq("X-myFilter-Time"), anyString());
    }

    @Test
    public void shouldNotReturnTimeSinceEnterCallAndShouldNotAddHeaderToResponse() throws Exception{
        boolean trace = false;
        boolean addHeader = true;

        long time;
        RequestTracer rt;

        response = mock(HttpServletResponse.class);

        rt = new RequestTracer(trace, addHeader);
        rt.traceEnter();
        Thread.sleep(1000L);
        time = rt.traceExit(response, "myFilter");

        assertThat("The returned time should be 0", time, equalTo(0L));
        // The x-trace-request header should not have been added to the response
        verify(response, never()).addHeader(eq("X-myFilter-Time"), anyString());

        trace = false;
        addHeader = false;

        response = mock(HttpServletResponse.class);

        rt = new RequestTracer(trace, addHeader);
        rt.traceEnter();
        Thread.sleep(1000L);
        time = rt.traceExit(response, "myFilter");

        assertThat("The returned time should be 0", time, equalTo(0L));
        // The x-trace-request header should not have been added to the response
        verify(response, never()).addHeader(eq("X-myFilter-Time"), anyString());
    }

    @Test
    public void shouldNotLogProcessingTime() {
        RequestTracer rt = new RequestTracer(true, false);

        response = mock(HttpServletResponse.class);

        rt.traceEnter();
        rt.traceExit(response, "myFilter");

        List<String> messageList = listAppender.getEvents().stream()
            .map(event -> event.getMessage().getFormattedMessage())
            .collect(Collectors.toList());
        assertThat(messageList, not(Matchers.contains(Matchers.startsWith("Filter myFilter spent"))));
    }

    @Test
    public void shouldLogProcessingTime() {
        RequestTracer rt = new RequestTracer(true, true);

        response = mock(HttpServletResponse.class);

        rt.traceEnter();
        rt.traceExit(response, "myFilter");

        List<String> messageList = listAppender.getEvents().stream()
            .map(event -> event.getMessage().getFormattedMessage())
            .collect(Collectors.toList());
        assertThat(messageList, Matchers.contains(Matchers.startsWith("Filter myFilter spent")));
    }
}
