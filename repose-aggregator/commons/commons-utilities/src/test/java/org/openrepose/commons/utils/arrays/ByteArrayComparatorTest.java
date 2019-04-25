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
package org.openrepose.commons.utils.arrays;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ByteArrayComparatorTest {

    @Test
    public void shouldReturnFalseForArraysWithDifferingSizes() {
        final byte[] first = new byte[]{0x1, 0x2, 0x3}, second = new byte[]{0x1, 0x2};

        assertFalse("Arrays that have different sizes should return false for asserting that they are equal", new ByteArrayComparator(first, second).arraysAreEqual());
    }

    @Test
    public void shouldReturnFalseForArraysWithDifferingContents() {
        final byte[] first = new byte[]{0x1, 0x2, 0x3}, second = new byte[]{0x1, 0x2, 0x5};

        assertFalse("Arrays that have different contents should return false for asserting that they are equal", new ByteArrayComparator(first, second).arraysAreEqual());
    }

    @Test
    public void shouldIdentifyIdenticalArrays() {
        final byte[] first = new byte[]{0x1, 0x2, 0x3}, second = new byte[]{0x1, 0x2, 0x3};

        assertTrue("Arrays that are identical should return true for asserting that they are equal", new ByteArrayComparator(first, second).arraysAreEqual());
    }
}
