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
package org.openrepose.filters.clientauth.atomfeed.sax;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.commons.utils.http.ServiceClient;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient;
import org.openrepose.filters.clientauth.atomfeed.CacheKeys;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SaxAuthFeedReaderTest {
    private static final String REPOSE_VERSION = "0.0.0.0";
    private ServiceClient client;
    private AkkaServiceClient akkaClient;
    private ServiceClientResponse resp1, resp2, resp3;
    private SaxAuthFeedReader reader;

    private ListAppender app;

    @Before
    public void setUp() throws FileNotFoundException {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        app = ((ListAppender) (ctx.getConfiguration().getAppender("List0"))).clear();

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
        reader = new SaxAuthFeedReader(client, akkaClient, REPOSE_VERSION, "http://some.junit.test.feed/at/somepath", "atomId", false);
        CacheKeys keys = reader.getCacheKeys("key");

        String[] users = {"224277258", //User from atom feed
                "4a2b42f4-6c63-11e1-815b-7fcbcf67f549"}; //TRR User from feed
        String[] tokens = {"834d3be1-c479-11e2-8b8b-0800200c9a66"};

        assertArrayEquals("Retrieved key should have user from atom feed", keys.getUserKeys().toArray(), users);
        assertArrayEquals("Retrieved keys should have token from atom feed", keys.getTokenKeys().toArray(), tokens);
    }

    @Test
    public void shouldLogUnauthorizedFeeds() throws Exception {

        resp3 = new ServiceClientResponse(401, null);
        when(client.get(eq("http://some.junit.test.feed/at/somepath"), anyMap())).thenReturn(resp3);
        reader = new SaxAuthFeedReader(client, akkaClient, REPOSE_VERSION, "http://some.junit.test.feed/at/somepath", "atomId", false);
        CacheKeys keys = reader.getCacheKeys("key");

        assertThat("Should log 401 with atom feed configured without auth",
                app.getMessages(), hasItem(containsString("Feed at http://some.junit.test.feed/at/somepath requires Authentication. Please reconfigure Feed atomId with valid credentials and/or configure isAuthed to true")));
    }

    @Test
    public void shouldLogServerErrorFromAtomFeeds() throws Exception {

        resp3 = new ServiceClientResponse(503, null);
        when(client.get(eq("http://some.junit.test.feed/at/somepath"), anyMap())).thenReturn(resp3);
        reader = new SaxAuthFeedReader(client, akkaClient, REPOSE_VERSION, "http://some.junit.test.feed/at/somepath", "atomId", false);

        CacheKeys keys = reader.getCacheKeys("key");

        assertThat(app.getMessages(), hasItem(containsString("Unable to retrieve atom feed from FeedatomId: http://some.junit.test.feed/at/somepath\n Response Code: 503")));
    }

    @Test
    public void shouldLogFeedNotFoundAndResetTargetToHead() throws Exception {
        resp3 = new ServiceClientResponse(404, null);
        when(client.get(eq("http://some.junit.test.feed/at/somepath"), anyMap())).thenReturn(resp1);
        when(client.get(eq("https://test.feed.atomhopper.rackspace.com/some/identity/feed/?marker=urn:uuid:b23a9c7f-5489-4fd8-bf10-3292032d805f&limit=25&search=&direction=forward"),
                anyMap())).thenReturn(resp2);
        reader = new SaxAuthFeedReader(client, akkaClient, REPOSE_VERSION, "http://some.junit.test.feed/at/somepath", "atomId", false);
        CacheKeys keys = reader.getCacheKeys("key");

        String[] users = {"224277258", //User from atom feed
                "4a2b42f4-6c63-11e1-815b-7fcbcf67f549"}; //TRR User from feed
        String[] tokens = {"834d3be1-c479-11e2-8b8b-0800200c9a66"};

        assertArrayEquals("Retrieved key should have user from atom feed", keys.getUserKeys().toArray(), users);
        assertArrayEquals("Retrieved keys should have token from atom feed", keys.getTokenKeys().toArray(), tokens);

        when(client.get(eq("https://test.feed.atomhopper.rackspace.com/some/identity/feed/?marker=urn:uuid:b23a9c7f-5489-4fd8-bf10-3292032d805f&limit=25&search=&direction=forward"),
                anyMap())).thenReturn(resp3);

        reader.getCacheKeys("key");

        assertThat(app.getMessages(), hasItem(containsString("Feed atomId not found at: https://test.feed.atomhopper.rackspace.com/some/" +
                "identity/feed/?marker=urn:uuid:b23a9c7f-5489-4fd8-bf10-3292032d805f&limit=25&search=&direction=forward" +
                "\nResetting feed target to: http://some.junit.test.feed/at/somepath")));
    }
}
