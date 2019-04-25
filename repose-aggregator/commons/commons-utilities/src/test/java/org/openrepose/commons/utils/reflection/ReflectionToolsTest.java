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
package org.openrepose.commons.utils.reflection;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: May 2, 2011
 * Time: 12:05:41 PM
 */
public class ReflectionToolsTest {

    private Constructor<SimpleClass> simpleClassConstructor;

    @Before
    public void setup() {
        simpleClassConstructor = null;
    }

    @Test
    public void shouldCorrectlyMatchParamterLists() {
        assertEquals("A Magical String", ReflectionTools.construct(String.class, "A Magical String"));
    }

    @Test
    public void shouldSupportEmptyConstructors() {
        assertEquals("", ReflectionTools.construct(String.class));
    }

    @Test
    public void shouldCorrectlyMatchParamterListsWithNull() {
        Exception ex = ReflectionTools.construct(Exception.class, null, null);

        assertNotNull("should not be null", ex);

        assertNull("should not have message", ex.getMessage());
        assertNull("should not have cause", ex.getCause());
    }

    @Test
    public void shouldConstructWithoutParameters() throws Exception {
        assertNotNull(ReflectionTools.construct(String.class));
    }

    @Test(expected = ReflectionException.class)
    public void whenConstructingObjectsViaReflectionShouldThrowExceptionIfConstructorIsNotFound() throws NoSuchMethodException {
        ReflectionTools.construct(SimpleClass.class, 42, "won't work");
    }

    @Test
    public void shouldReturnConstructorsWithMatchingSignature() throws NoSuchMethodException {
        Class<?>[] typeArray = {String.class, Integer.class};

        simpleClassConstructor = ReflectionTools.getConstructor(SimpleClass.class, typeArray);

        assertThat(simpleClassConstructor.toString(), containsString("SimpleClass(java.lang.String,java.lang.Integer)"));
    }

    @Test(expected = NoSuchMethodException.class)
    public void whenGettingConstructorsShouldThrowExceptionIfConstructorIsNotFound() throws NoSuchMethodException {
        Class<?>[] typeArray = {Integer.class, String.class};

        ReflectionTools.getConstructor(SimpleClass.class, typeArray);
    }

    @Test
    public void shouldSupportEmptyParams() throws NoSuchMethodException {
        Class<?>[] typeArray = new Class<?>[0];

        simpleClassConstructor = ReflectionTools.getConstructor(SimpleClass.class, typeArray);

        assertThat(simpleClassConstructor.toString(), containsString("SimpleClass()"));
    }

    @Test
    public void shouldReturnArrayOfAssociatedClasses() {
        Integer i = 42;
        String s = "string";
        Double d = 101.5;
        Class[] actual;

        actual = ReflectionTools.toClassArray(i, s, d, null);

        assertEquals("integer", Integer.class, actual[0]);
        assertEquals("string", String.class, actual[1]);
        assertEquals("double", Double.class, actual[2]);
        assertNull("null", actual[3]);
    }

    @Test
    public void shouldNotFailOnNullReferences() {
        Class[] actual;
        Object obj = null;

        actual = ReflectionTools.toClassArray(obj);

        assertNull(actual[0]);
    }

    static class SimpleClass {
        private final String x;
        private final Integer y;

        public SimpleClass() {
            this.x = "none";
            this.y = -1;
        }

        public SimpleClass(String x, Integer y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "[" + x + "]: " + y;
        }
    }
}
