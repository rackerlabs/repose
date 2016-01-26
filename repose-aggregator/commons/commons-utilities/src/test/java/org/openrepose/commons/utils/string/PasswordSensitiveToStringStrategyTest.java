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
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author kush5342
 */
public class PasswordSensitiveToStringStrategyTest {


    /**
     * Test of appendField method, of class PasswordSensitiveToStringStrategy.
     */
    @Test
    public void testAppendField() {

        ObjectLocator objectLocator = mock(ObjectLocator.class);
        Object o = null;
        String s = "abcd";
        StringBuilder stringBuilder = new StringBuilder();
        Object o1 = mock(Object.class);
        PasswordSensitiveToStringStrategy instance = new PasswordSensitiveToStringStrategy();
        StringBuilder result = instance.appendField(objectLocator, o, "password", stringBuilder, o1);
        assertThat(result.toString(), equalTo("password=*******, "));

    }

    @Test
    public void testAppendFieldNotPassword() {

        ObjectLocator objectLocator = mock(ObjectLocator.class);
        Object o = null;
        String s = "abcd";
        StringBuilder stringBuilder = new StringBuilder();
        Object o1 = mock(Object.class);
        PasswordSensitiveToStringStrategy instance = new PasswordSensitiveToStringStrategy();
        StringBuilder result = instance.appendField(objectLocator, o, "field", stringBuilder, o1);
        assertThat(result.toString(), equalTo("field=" + o1.toString() + ", "));

    }

}
