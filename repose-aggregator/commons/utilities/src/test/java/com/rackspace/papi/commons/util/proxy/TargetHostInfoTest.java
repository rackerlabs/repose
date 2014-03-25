package com.rackspace.papi.commons.util.proxy;

import org.junit.Test;

import java.net.URI;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TargetHostInfoTest {
    private final String targetHost = "http://otherhost.com/mocks/test";
    private final String invalidTargetHost = "abcd?%;:,==some weird string";

    @Test
    public void getProxiedHostUri_returnsExpectedUri() throws Exception {
        TargetHostInfo targetHostInfo = new TargetHostInfo(targetHost);

        assertEquals("Returned URI was not the expected URI", new URI(targetHost), targetHostInfo.getProxiedHostUri());
    }

    @Test
    public void getProxiedHostUri_returnsNullOnInvalidUri() throws Exception {
        TargetHostInfo targetHostInfo = new TargetHostInfo(invalidTargetHost);

        assertNull("Returned URI was not null as expected", targetHostInfo.getProxiedHostUri());
    }

    @Test
    public void getProxiedHostUrl_returnsExpectedUrl() throws Exception {
        TargetHostInfo targetHostInfo = new TargetHostInfo(targetHost);

        assertEquals("Returned URL was not the expected URL", new URL("http", "otherhost.com", -1, ""), targetHostInfo.getProxiedHostUrl());
    }

    @Test
    public void getProxiedHostUrl_returnsNullOnInvalidUrl() throws Exception {
        TargetHostInfo targetHostInfo = new TargetHostInfo(invalidTargetHost);

        assertNull("Returned URL was not null as expected", targetHostInfo.getProxiedHostUrl());
    }
}
