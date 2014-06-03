package com.rackspace.papi.service.event;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;


import java.util.LinkedList;

import static org.junit.Assert.*;





@RunWith(Enclosed.class)
public class ComparableClassWrapperTest {

    public static class testCompWrapper {

        @Test
        public void testNonNullHash() {
            Integer num = new Integer(5);
            ComparableClassWrapper<Number> wrap = new ComparableClassWrapper<Number>(num.getClass());
            assertTrue(wrap.hashCode() == (7 *89) + num.getClass().hashCode());

        }

        @Test
        public void testNullWrappedHash() {
            ComparableClassWrapper<Enum> wrap = new ComparableClassWrapper<Enum>(null);
            assertTrue(wrap.hashCode() == (7 *89));
        }

        @Test
        public void testEqualsDifferentClass() {
            Integer num = new Integer(5);
            ComparableClassWrapper<Number> wrap = new ComparableClassWrapper<Number>(num.getClass());
            LinkedList<Integer> list = new LinkedList<Integer>();
            assertFalse(wrap.equals(list));
        }

        @Test
        public void test4() {
            Integer num = new Integer(5);
            ComparableClassWrapper<Number> wrap = new ComparableClassWrapper<Number>(num.getClass());
            Double num2 = new Double(5.0);
            ComparableClassWrapper<Number> wrap2 = new ComparableClassWrapper<Number>(num2.getClass());
            assertFalse(wrap.equals(wrap2));
        }
    }
}
