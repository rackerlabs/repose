package com.rackspace.papi.commons.util.logging.apache.format.converters

import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat
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
        assertThat(result, equalTo(null))
    }

    @Test
    void "whitespace input value passes straight through"() {
        def emptyString = '   '
        String result = converter.convert(emptyString, null, null)
        assertThat(result, equalTo(emptyString))
    }

    @Test
    void "some input value but empty output format returns value"() {
        def testValue = "some value, not really a date, but it doesn't matter since we aren't getting that far"
        String result = converter.convert(testValue, null, ' ')
        assertThat(result, equalTo(testValue))
    }

    @Test
    void "invalid input value with valid formats passes through"() {
        def testValue = "not a date"
        String result = converter.convert(testValue, 'ISO_8601', 'RFC_1123')
        assertThat(result, equalTo(testValue))
    }

    @Test
    void "valid input gets converted correctly"() {
        def testValue = "1994-11-05T13:15:30Z"
        String result = converter.convert(testValue, 'ISO_8601', 'RFC_1123')
        assertThat(result, equalTo('Sat, 05 Nov 1994 13:15:30 GMT'))
    }

    @Test
    void "valid input value with a bad output format defaults to rfc-1123"() {
        def testValue = "1994-11-05T13:15:30Z"
        String result = converter.convert(testValue, 'ISO_8601', 'squirrel noises')
        assertThat(result, equalTo('Sat, 05 Nov 1994 13:15:30 GMT'))
    }

    @Test
    void "valid input value with a bad input format defaults to rfc-1123"() {
        def testValue = "Sat, 05 Nov 1994 13:15:30 GMT"
        String result = converter.convert(testValue, 'squirrel noises', 'ISO_8601')
        assertThat(result, equalTo('1994-11-05T13:15:30Z'))
    }
}
