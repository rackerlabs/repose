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
package org.openrepose.commons.utils.string;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author kush5342
 */
public class StringBuilderWrapperTest {


    static StringBuilderWrapper instance;

    @BeforeClass
    public static void setUpClass() {
        instance = new StringBuilderWrapper(new StringBuilder("test"));
    }

    /**
     * Test of indexOf method, of class StringBufferWrapper.
     */
    @Test
    public void testIndexOf_String() {

        String seq = "st";

        int expResult = 2;
        int result = instance.indexOf(seq);
        assertEquals(expResult, result);

    }

    /**
     * Test of indexOf method, of class StringBufferWrapper.
     */
    @Test
    public void testIndexOf_String_int() {

        String seq = "t";
        int fromIndex = 2;

        int expResult = 3;
        int result = instance.indexOf(seq, fromIndex);
        assertEquals(expResult, result);

    }

    /**
     * Test of asCharSequence method, of class StringBufferWrapper.
     */
    @Test
    public void testAsCharSequence() {


        CharSequence result = instance.asCharSequence();
        assertEquals("test", result.toString());

    }

    /**
     * Test of charAt method, of class StringBufferWrapper.
     */
    @Test
    public void testCharAt() {

        int i = 0;

        char expResult = 't';
        char result = instance.charAt(i);
        assertEquals(expResult, result);

    }

    /**
     * Test of length method, of class StringBufferWrapper.
     */
    @Test
    public void testLength() {

        int expResult = 4;
        int result = instance.length();
        assertEquals(expResult, result);

    }

    /**
     * Test of subSequence method, of class StringBufferWrapper.
     */
    @Test
    public void testSubSequence() {

        int i = 1;
        int i1 = 3;


        CharSequence result = instance.subSequence(i, i1);
        assertEquals("es", result.toString());

    }
}
