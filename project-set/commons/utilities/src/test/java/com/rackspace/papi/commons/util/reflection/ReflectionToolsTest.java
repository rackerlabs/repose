package com.rackspace.papi.commons.util.reflection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;

import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: May 2, 2011
 * Time: 12:05:41 PM
 */
@RunWith(Enclosed.class)
public class ReflectionToolsTest {
    public static class WhenConstructingObjectsViaReflection {

        @Test
        public void shouldCorrectlyMatchParamterLists() {
            Assert.assertEquals("A Magical String", ReflectionTools.construct(String.class, "A Magical String"));
        }

        @Test
        public void shouldSupportEmptyConstructors() {
            Assert.assertEquals("", ReflectionTools.construct(String.class));
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

        @Test(expected=ReflectionException.class)
        public void shouldThrowExceptionIfConstructorIsNotFound() throws NoSuchMethodException {
            ReflectionTools.construct(SimpleClass.class, 42, "won't work");
        }
    }

    public static class WhenGettingConstructors {
        private Constructor<SimpleClass> simpleClassConstructor;

        @Before
        public void setup() {
            simpleClassConstructor = null;
        }

        @Test
        public void shouldReturnConstructorsWithMatchingSignature() throws NoSuchMethodException {
            Class<?>[] typeArray = { String.class, Integer.class };

            simpleClassConstructor = ReflectionTools.getConstructor(SimpleClass.class, typeArray);

            assertTrue(simpleClassConstructor.toString().contains("SimpleClass(java.lang.String,java.lang.Integer)"));
        }

        @Test(expected=NoSuchMethodException.class)
        public void shouldThrowExceptionIfConstructorIsNotFound() throws NoSuchMethodException {
            Class<?>[] typeArray = { Integer.class, String.class };

            ReflectionTools.getConstructor(SimpleClass.class, typeArray);
        }

        @Test
        public void shouldSupportEmptyParams() throws NoSuchMethodException {
            Class<?>[] typeArray = new Class<?>[0];

            simpleClassConstructor = ReflectionTools.getConstructor(SimpleClass.class, typeArray);

            assertTrue(simpleClassConstructor.toString().contains("SimpleClass()"));
        }
    }

    public static class WhenConvertingObjectParamListToClassArray {
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
