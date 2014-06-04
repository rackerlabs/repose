package com.rackspace.papi.components.translation.xslt;


import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class StyleSheetInfoTest {

    @Test
    public void testNodeProvidedConstructor() {
        String id = "id";
        String systemId = "sysID";
        StyleSheetInfo sheet = new StyleSheetInfo("id",null,"sysID");

        assertTrue(sheet.getId().equals("id"));
        assertNull(sheet.getXsl());
        assertNull(sheet.getUri());
        assertTrue(sheet.getSystemId().equals("sysID"));
    }
}

