package com.rackspace.papi.commons.util.http;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExtendedHttpHeaderTest {
    
    public ExtendedHttpHeaderTest() {
    }
    
    @Test
    public void shouldMakeManagementHappy() {
        ExtendedHttpHeader header = ExtendedHttpHeader.X_TTL;
        assertNotNull(header);
        assertEquals("x-ttl", header.toString());
        assertTrue(header.matches("x-ttl"));
    }

}
