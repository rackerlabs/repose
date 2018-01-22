package org.openrepose.core.services.opentracing

import org.apache.commons.codec.binary.Base64
import org.junit.Test

import static org.junit.Assert.*


class TraceGUIDExtractorTest {

    @Test
    void testConstructorWithEmptyString() {
        String traceHeaderValue = "";

        TraceGUIDExtractor traceGUIDExtractor = new TraceGUIDExtractor(
                traceHeaderValue)
        assertTrue("Validate tracing map was not populated",
                traceGUIDExtractor.tracingMap.isEmpty())

    }

    @Test
    void testConstructorWithInvalidBase64String() {
        String traceHeaderValue = "thisisnotbase64!";

        TraceGUIDExtractor traceGUIDExtractor = new TraceGUIDExtractor(
                traceHeaderValue)
        assertTrue("Validate tracing map was not populated",
                traceGUIDExtractor.tracingMap.isEmpty())

    }

    @Test
    void testConstructorWithInvalidJson() {
        String traceHeaderValue = new String(Base64.encodeBase64("notvalidjson".bytes))

        TraceGUIDExtractor traceGUIDExtractor = new TraceGUIDExtractor(
                traceHeaderValue)
        assertTrue("Validate tracing map was not populated",
                traceGUIDExtractor.tracingMap.isEmpty())

    }

    @Test
    void testConstructorWithValidJson() {
        String traceHeaderValue = new String(
                Base64.encodeBase64("{\"id\":12345,\"origin\":null}".bytes))

        TraceGUIDExtractor traceGUIDExtractor = new TraceGUIDExtractor(
                traceHeaderValue)
        assertTrue("Validate tracing map was populated with id",
                traceGUIDExtractor.tracingMap.containsKey("id"))
        assertTrue("Validate tracing map was populated with origin",
                traceGUIDExtractor.tracingMap.containsKey("origin"))
        assertEquals("Validate id key is set to 12345 value",
                traceGUIDExtractor.tracingMap.get("id"), "12345")
        assertEquals("Validate origin key is set to null",
                traceGUIDExtractor.tracingMap.get("origin"), null)

    }

    @Test
    void testIteratorWithEmptyTracingMap() {
        String traceHeaderValue = "";

        TraceGUIDExtractor traceGUIDExtractor = new TraceGUIDExtractor(
                traceHeaderValue)

        assertFalse("Validate iterator is empty",
                traceGUIDExtractor.iterator().hasNext())
    }

    @Test
    void testIteratorWithNonEmptyTracingMap() {
        String traceHeaderValue = new String(
                Base64.encodeBase64("{\"id\":12345,\"origin\":null}".bytes))

        TraceGUIDExtractor traceGUIDExtractor = new TraceGUIDExtractor(
                traceHeaderValue)

        assertEquals("Validate iterator has two values",
                traceGUIDExtractor.iterator().size(), 2)

        assertEquals("Validate next value is id",
                traceGUIDExtractor.iterator().next().key, "id"
        )
    }

    @Test(expected = UnsupportedOperationException.class)
    void testPutThrowsInvalidOperation() {
        String traceHeaderValue = new String(
                Base64.encodeBase64("{\"id\":12345,\"origin\":null}".bytes))

        TraceGUIDExtractor traceGUIDExtractor = new TraceGUIDExtractor(
                traceHeaderValue)
        traceGUIDExtractor.put("key", "value")
    }
}
