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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ArrayUtilitiesTest {

    @Test
    public void testNullSafeCopyNull() {
        assertThat(ArrayUtilities.nullSafeCopy((Object[]) null), equalTo(null));
    }

    @Test
    public void testNullSafeCopyNonNull() {
        String[] array = {"element1", "element2"};
        assertThat(ArrayUtilities.nullSafeCopy(array), equalTo(array));
    }

    @Test
    public void testNullSafeCopyNullByte() {
        assertThat(ArrayUtilities.nullSafeCopy((byte[]) null), equalTo(null));
    }

    @Test
    public void testNullSafeCopyNonNullByte() {
        byte[] array = "array".getBytes();
        assertThat(ArrayUtilities.nullSafeCopy(array), equalTo(array));
    }

}
