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
package org.openrepose.commons.utils;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author fran
 */
public class SetUtilitiesTest {
    @Test
    public void shouldReturnFalseIfFirstSetIsNull() {
        Set<String> one = null;
        Set<String> two = new HashSet();
        two.add("abc");

        assertFalse(SetUtilities.nullSafeEquals(one, two));
    }

    @Test
    public void shouldReturnFalseIfSecondSetIsNull() {
        Set<String> one = new HashSet();
        one.add("abc");
        Set<String> two = null;

        assertFalse(SetUtilities.nullSafeEquals(one, two));
    }

    @Test
    public void shouldReturnFalseIfNonNullSetsAreDifferent() {
        Set<String> one = new HashSet();
        one.add("abc");
        Set<String> two = new HashSet();
        two.add("def");

        assertFalse(SetUtilities.nullSafeEquals(one, two));
    }

    @Test
    public void shouldReturnTrueIfBothSetsAreNull() {
        Set<String> one = null;
        Set<String> two = null;

        assertTrue(SetUtilities.nullSafeEquals(one, two));
    }

    @Test
    public void shouldReturnTrueIfNonNullSetsAreSame() {
        Set<String> one = new HashSet();
        one.add("abc");
        Set<String> two = new HashSet();
        two.add("abc");

        assertTrue(SetUtilities.nullSafeEquals(one, two));
    }

    @Test
    public void shouldReturnFalseIfNonNullSetsAreSameButDifferentCase() {
        Set<String> one = new HashSet();
        one.add("abc");
        Set<String> two = new HashSet();
        two.add("AbC");

        assertFalse(SetUtilities.nullSafeEquals(one, two));
    }
}
