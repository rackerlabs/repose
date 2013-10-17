package com.rackspace.papi.commons.util;

import com.rackspace.papi.commons.util.reflection.ReflectionTools;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 *
 * 
 */
@RunWith(Enclosed.class)
public class ReflectionToolsTest {

    public static class WhenConstructingObjectsViaReflection {

        @Test
        public void shouldCorrectlyMatchParamterLists() {
            assertEquals("A Magical String", ReflectionTools.construct(String.class, "A Magical String"));
        }

        @Test
        public void shouldCorrectlyMatchParamterListsWithNull() {
            Exception ex = ReflectionTools.construct(Exception.class, null, null);

            assertNotNull(ex);

            assertNull(ex.getMessage());
            assertNull(ex.getCause());
        }

        @Test
        public void shouldConstructWithoutParameters() throws Exception {
            assertNotNull(ReflectionTools.construct(String.class));
        }
    }
}
