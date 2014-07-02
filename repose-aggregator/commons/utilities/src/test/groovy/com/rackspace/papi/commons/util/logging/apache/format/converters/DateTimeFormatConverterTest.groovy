package com.rackspace.papi.commons.util.logging.apache.format.converters
import org.junit.Assume
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
        assert result ==null
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

    /*
     * The format we're using is incorrect and doesn't do what it says it does,
     * the following two tests prove it.
     *
     * Unfortunately the SimpleDateFormat class in Java 1.6 is broken
     * The splodeDate is set to July of 2014, which is about when we expect to be switching to JDK 1.7
     * At this point the tests will execute, and probably fail unless we've switched to JDK 1.7, at which point they
     * should succeed!
     *
     * As long as the assumption proves false, it will ignore the tests. So we're assuming that we're past the timebomb
     * date, otherwise the test method is ignored
     */
    def splodeDate = new Date(2014 - 1900, Calendar.AUGUST, 1, 9, 0);

    @Test
    void "valid input gets converted correctly"() {
        Assume.assumeTrue(new Date() > splodeDate)

        def testValue = "1994-11-05T13:15:30Z"
        String result = converter.convert(testValue, 'ISO_8601', 'RFC_1123')
        assert result.equals('Sat, 05 Nov 1994 13:15:30 GMT')
    }

    @Test
    void "valid input value with a bad output format defaults to rfc-1123"() {
        Assume.assumeTrue(new Date() > splodeDate)

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
