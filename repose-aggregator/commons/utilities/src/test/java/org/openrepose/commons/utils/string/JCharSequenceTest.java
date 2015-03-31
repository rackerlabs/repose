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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author kush5342
 */
public class JCharSequenceTest {


    /**
     * Test of asCharSequence method, of class JCharSequence.
     */
    @Test
    public void testAsCharSequence() {

        JCharSequence instance = new StringWrapper("test");
        CharSequence result = instance.asCharSequence();
        assertEquals("test", result.toString());

    }

    /**
     * Test of indexOf method, of class JCharSequence.
     */
    @Test
    public void testIndexOf_String() {
        System.out.println("indexOf");
        String seq = "st";
        JCharSequence instance = new StringWrapper("test");

        int result = instance.indexOf(seq);
        assertEquals(2, result);

    }

    /**
     * Test of indexOf method, of class JCharSequence.
     */
    @Test
    public void testIndexOf_String_int() {

        String seq = "st";
        int fromIndex = 4;
        JCharSequence instance = new StringWrapper("testtest");
        int expResult = 0;
        int result = instance.indexOf(seq, fromIndex);
        assertEquals(6, result);

    }


}
