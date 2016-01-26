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
package org.openrepose.commons.utils.logging.apache.format.converters

import org.junit.Test

/**
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 9/25/13
 * Time: 9:31 AM
 */
class DateTimeFormatConverterTest {
    DateTimeFormatConverter converter = new DateTimeFormatConverter()

    @Test
    void "null input value passes straight through"() {
        String result = converter.convert(null, null, null)
        assert result == null
    }

    @Test
    void "whitespace input value passes straight through"() {
        def emptyString = '   '
        String result = converter.convert(emptyString, null, null)
        assert result.equals(emptyString)
    }

    @Test
    void "some input value but empty output format returns value"() {
        def testValue = "some value, not really a date, but it doesn't matter since we aren't getting that far"
        String result = converter.convert(testValue, null, ' ')
        assert result.equals(testValue)
    }

    @Test
    void "invalid input value with valid formats passes through"() {
        def testValue = "not a date"
        String result = converter.convert(testValue, 'ISO_8601', 'RFC_1123')
        assert result.equals(testValue)
    }

    @Test
    void "valid input gets converted correctly"() {
        def testValue = "1994-11-05T13:15:30Z"
        String result = converter.convert(testValue, 'ISO_8601', 'RFC_1123')
        assert result.equals('Sat, 05 Nov 1994 13:15:30 GMT')
    }

    @Test
    void "valid input value with a bad output format defaults to rfc-1123"() {
        def testValue = "1994-11-05T13:15:30Z"
        String result = converter.convert(testValue, 'ISO_8601', 'squirrel noises')
        assert (result.equals('Sat, 05 Nov 1994 13:15:30 GMT'))
    }

    @Test
    void "valid input value with a bad input format defaults to rfc-1123"() {
        def testValue = "Sat, 05 Nov 1994 13:15:30 GMT"
        String result = converter.convert(testValue, 'squirrel noises', 'ISO_8601')
        assert (result.equals('1994-11-05T13:15:30Z'))
    }
}
