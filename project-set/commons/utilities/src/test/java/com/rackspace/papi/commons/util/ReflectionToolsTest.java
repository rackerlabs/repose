/*
 *  Copyright 2010 Rackspace.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
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
