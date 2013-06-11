package com.rackspace.papi.service.proxy.jersey;

import com.rackspace.papi.commons.util.logging.jersey.LoggingFilter;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class ClientWrapperTest {

    public static class WhenInstantiating {

        private Client client;
        private ClientWrapper wrapper;
        private WebResource resource;

        @Before
        public void setUp() {
            client = mock(Client.class);
            resource = mock(WebResource.class);
        }

        @Test
        public void shouldNotInstallLoggingFilter() {
            wrapper = new ClientWrapper(client, false, null);
            verify(client, times(0)).addFilter(any(LoggingFilter.class));
        }

        @Test
        public void shouldInstallLoggingFilter() {
            wrapper = new ClientWrapper(client, true, null);
            verify(client, times(1)).addFilter(any(LoggingFilter.class));
        }
    }

    public static class WhenCachingResources {

        private Client client;
        private ClientWrapper wrapper;
        private WebResource resource;

        @Before
        public void setUp() {
            client = mock(Client.class);
            resource = mock(WebResource.class);

            wrapper = new ClientWrapper(client, false, null);
        }

        @Test
        public void shouldStoreResourcesInShortTermCache() throws InterruptedException {
            final String url = "someurl";
            when(client.resource(eq(url))).thenReturn(resource);
            WebResource actual = wrapper.resource(url);
            assertEquals(resource, actual);

            verify(client).resource(eq(url));
            wrapper.resource(url);
            verify(client).resource(eq(url));

            Thread.sleep(2000);
            wrapper.resource(url);
            // Cache should have expired
            verify(client, times(2)).resource(eq(url));
        }

        @Test
        public void shouldStoreResourcesInLongTermCache() throws InterruptedException {
            final String url = "someurl";
            when(client.resource(eq(url))).thenReturn(resource);
            WebResource actual = wrapper.resource(url, true);
            assertEquals(resource, actual);

            verify(client).resource(eq(url));
            wrapper.resource(url, true);
            verify(client).resource(eq(url));

            Thread.sleep(10000);
            wrapper.resource(url, true);
            // Should still have resource in cache
            verify(client, times(1)).resource(eq(url));
        }
    }
}
