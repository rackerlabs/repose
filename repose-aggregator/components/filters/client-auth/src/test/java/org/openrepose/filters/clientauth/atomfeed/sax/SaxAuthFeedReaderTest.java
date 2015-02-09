package org.openrepose.filters.clientauth.atomfeed.sax;

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
import org.openrepose.filters.clientauth.atomfeed.CacheKeys;
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SaxAuthFeedReaderTest {
    private ServiceClient client;
    private AkkaServiceClient akkaClient;
    private ServiceClientResponse resp1, resp2, resp3;
    private SaxAuthFeedReader reader;

    private ListAppender app;

    @Before
    public void setUp() throws FileNotFoundException {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        app = ((ListAppender)(ctx.getConfiguration().getAppender("List0"))).clear();

        client = mock(ServiceClient.class);
        akkaClient = mock(AkkaServiceClient.class);

        InputStream fileReader1 = getClass().getResourceAsStream(File.separator + "META-INF" + File.separator + "feed.xml");
        InputStream fileReader2 = getClass().getResourceAsStream(File.separator + "META-INF" + File.separator + "empty-feed.xml");
        resp1 = new ServiceClientResponse(200, fileReader1);

        resp2 = new ServiceClientResponse(200, fileReader2);
        when(client.getPoolSize()).thenReturn(100);

    }

    @Test
    public void shouldRetrieveUserAndTokenKeysFromAtomFeed() throws Exception {

        when(client.get(eq("http://some.junit.test.feed/at/somepath"), anyMap())).thenReturn(resp1);
        when(client.get(eq("https://test.feed.atomhopper.rackspace.com/some/identity/feed/?marker=urn:uuid:b23a9c7f-5489-4fd8-bf10-3292032d805f&limit=25&search=&direction=forward"),
                anyMap())).thenReturn(resp2);
        reader = new SaxAuthFeedReader(client, akkaClient, "http://some.junit.test.feed/at/somepath", "atomId");
        CacheKeys keys = reader.getCacheKeys();

        String[] users = {"224277258"}; //User from atom feed
        String[] tokens = {"834d3be1-c479-11e2-8b8b-0800200c9a66", //The token revocation event
                "4a2b42f4-6c63-11e1-815b-7fcbcf67f549"}; // The TRR user event

        assertArrayEquals("Retrieved key should have user from atom feed", keys.getUserKeys().toArray(), users);
        assertArrayEquals("Retrieved keys should have token from atom feed", keys.getTokenKeys().toArray(), tokens);
    }

    @Test
    public void shouldLogUnauthorizedFeeds() throws Exception {

        resp3 = new ServiceClientResponse(401, null);
        when(client.get(eq("http://some.junit.test.feed/at/somepath"), anyMap())).thenReturn(resp3);
        reader = new SaxAuthFeedReader(client, akkaClient, "http://some.junit.test.feed/at/somepath", "atomId");
        CacheKeys keys = reader.getCacheKeys();

        assertThat("Should log 401 with atom feed configured without auth",
                app.getEvents(), contains("Feed at http://some.junit.test.feed/at/somepath requires Authentication. Please reconfigure Feed atomId with valid credentials and/or configure isAuthed to true"));
    }

    @Test
    public void shouldLogServerErrorFromAtomFeeds() throws Exception {

        resp3 = new ServiceClientResponse(503, null);
        when(client.get(eq("http://some.junit.test.feed/at/somepath"), anyMap())).thenReturn(resp3);
        reader = new SaxAuthFeedReader(client, akkaClient, "http://some.junit.test.feed/at/somepath", "atomId");

        CacheKeys keys = reader.getCacheKeys();

        assertThat(app.getEvents(), contains("Unable to retrieve atom feed from FeedatomId: http://some.junit.test.feed/at/somepath\n Response Code: 503"));
    }

    private Matcher<List<LogEvent>> contains(final String msg) {
        return new TypeSafeMatcher<List<LogEvent>>() {
            @Override
            protected boolean matchesSafely(final List<LogEvent> events) {
                boolean rtn = false;
                LogEvent event;
                for(Iterator<LogEvent> iterator = events.iterator(); !rtn && iterator.hasNext();) {
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
