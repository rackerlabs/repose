package com.rackspace.papi.components.translation.xslt;


import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;



import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class StyleSheetInfoTest {

    public static class Constructor {

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
}

