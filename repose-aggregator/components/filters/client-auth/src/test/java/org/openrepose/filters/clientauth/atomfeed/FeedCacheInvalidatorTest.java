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
package org.openrepose.filters.clientauth.atomfeed;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.commons.utils.http.ServiceClient;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient;
import org.openrepose.filters.clientauth.atomfeed.sax.SaxAuthFeedReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class FeedCacheInvalidatorTest {
    private ServiceClient client;
    private AkkaServiceClient akkaClient;
    private ServiceClientResponse resp1, resp2, resp3;
    private SaxAuthFeedReader reader;
    private Datastore datastore;

    private ListAppender app;

    @Before
    public void setUp() throws FileNotFoundException {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        app = ((ListAppender) (ctx.getConfiguration().getAppender("List0"))).clear();

        client = mock(ServiceClient.class);
        akkaClient = mock(AkkaServiceClient.class);
        datastore = mock(Datastore.class);

        InputStream fileReader1 = getClass().getResourceAsStream(File.separator + "META-INF" + File.separator + "feed.xml");
        InputStream fileReader2 = getClass().getResourceAsStream(File.separator + "META-INF" + File.separator + "empty-feed.xml");
        resp1 = new ServiceClientResponse(200, fileReader1);

        resp2 = new ServiceClientResponse(200, fileReader2);
        when(client.getPoolSize()).thenReturn(100);

    }

    @Test
    public void shouldIncludeTraceInLog() {
        when(client.get(eq("http://some.junit.test.feed/at/somepath"), anyMap())).thenReturn(resp1);
        when(client.get(eq("https://test.feed.atomhopper.rackspace.com/some/identity/feed/?marker=urn:uuid:b23a9c7f-5489-4fd8-bf10-3292032d805f&limit=25&search=&direction=forward"),
                anyMap())).thenReturn(resp2);

        FeedCacheInvalidator fci = FeedCacheInvalidator.openStackInstance(datastore);
        //fci.run();
        fci.done();

        assertThat(app.getEvents(), contains("Beginning Feed Cache Invalidator Thread request."));
    }

    private Matcher<List<LogEvent>> contains(final String msg) {
        return new TypeSafeMatcher<List<LogEvent>>() {
            @Override
            protected boolean matchesSafely(final List<LogEvent> events) {
                boolean rtn = false;
                LogEvent event;
                for (Iterator<LogEvent> iterator = events.iterator(); !rtn && iterator.hasNext(); ) {
                    event = iterator.next();
                    rtn = event.getMessage().getFormattedMessage().contains(msg);
                }
                return rtn;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("The List of Log Events contained a Formatted Message of: \"" + msg + "\"");
            }
        };
    }
}
