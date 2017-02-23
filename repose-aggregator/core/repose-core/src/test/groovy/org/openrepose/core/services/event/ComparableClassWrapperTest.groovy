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
package org.openrepose.core.services.event

import org.junit.Test

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.not
import static org.junit.Assert.assertThat

public class ComparableClassWrapperTest {

    @Test
    public void testNonNullHash() {
        Integer num = new Integer(5);
        ComparableClassWrapper<Number> wrap = new ComparableClassWrapper<Number>(num.getClass());

        assertThat(wrap.hashCode(), equalTo((7 * 89) + num.getClass().hashCode()))
    }

    @Test
    public void testNullWrappedHash() {
        ComparableClassWrapper<Enum> wrap = new ComparableClassWrapper<Enum>(null);
        assertThat(wrap.hashCode(), equalTo(7 * 89));
    }

    @Test
    public void testEqualsDifferentClass() {
        Integer num = new Integer(5);
        ComparableClassWrapper<Number> wrap = new ComparableClassWrapper<Number>(num.getClass());
        LinkedList<Integer> list = new LinkedList<Integer>();
        assertThat(wrap, not(list));
    }

    @Test
    public void test4() {
        Integer num = new Integer(5);
        ComparableClassWrapper<Number> wrap = new ComparableClassWrapper<Number>(num.getClass());
        Double num2 = new Double(5.0);
        ComparableClassWrapper<Number> wrap2 = new ComparableClassWrapper<Number>(num2.getClass());
        assertThat(wrap, not(wrap2))
    }
}
