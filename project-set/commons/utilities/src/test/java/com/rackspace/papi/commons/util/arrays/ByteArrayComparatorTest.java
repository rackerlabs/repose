package com.rackspace.papi.commons.util.arrays;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class ByteArrayComparatorTest {

    public static class WhenComparingByteArrays {

        @Test
        public void shouldReturnFalseForArraysWithDifferingSizes() {           
            final byte[] first = new byte[] {0x1, 0x2, 0x3}, second = new byte[] {0x1, 0x2};
            
            assertFalse("Arrays that have different sizes should return false for asserting that they are equal", new ByteArrayComparator(first, second).arraysAreEqual());
        }

        @Test
        public void shouldReturnFalseForArraysWithDifferingContents() {           
            final byte[] first = new byte[] {0x1, 0x2, 0x3}, second = new byte[] {0x1, 0x2, 0x5};
            
            assertFalse("Arrays that have different contents should return false for asserting that they are equal", new ByteArrayComparator(first, second).arraysAreEqual());
        }
        
        @Test
        public void shouldIdentifyIdenticalArrays() {
            final byte[] first = new byte[] {0x1, 0x2, 0x3}, second = new byte[] {0x1, 0x2, 0x3};
            
            assertTrue("Arrays that are identical should return true for asserting that they are equal", new ByteArrayComparator(first, second).arraysAreEqual());
        }
    }
}
