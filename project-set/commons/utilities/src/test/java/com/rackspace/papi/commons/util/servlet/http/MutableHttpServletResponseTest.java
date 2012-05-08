package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.io.ByteBufferServletOutputStream;
import com.rackspace.papi.commons.util.io.buffer.CyclicByteBuffer;
import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class MutableHttpServletResponseTest {

    public static class WhenCreatingNewInstances {

        @Test
        public void shouldPassReferenceThroughIfIsWrapperInstance() {
            MutableHttpServletResponse original = MutableHttpServletResponse.wrap(mock(HttpServletResponse.class));
            MutableHttpServletResponse actual;

            actual = MutableHttpServletResponse.wrap(original);

            assertSame(original, actual);
        }

        @Test
        public void shouldCreateNewInstanceIfIsNotWrapperInstance() {
            HttpServletResponse original = mock(HttpServletResponse.class);
            MutableHttpServletResponse actual;

            actual = MutableHttpServletResponse.wrap(original);

            assertNotSame(original, actual);
        }

        @Test
        public void shouldHaveInitializedBuffer() {
            HttpServletResponse original = mock(HttpServletResponse.class);
            MutableHttpServletResponse actual;

            actual = MutableHttpServletResponse.wrap(original);

            assertEquals(2048, actual.getBufferSize());
        }

        @Test
        public void shouldHaveCreatedNewOutputStream() throws IOException {
            HttpServletResponse original = mock(HttpServletResponse.class);
            MutableHttpServletResponse actual;

            actual = MutableHttpServletResponse.wrap(original);

            assertTrue(actual.getOutputStream() instanceof ByteBufferServletOutputStream);
        }

        @Test
        public void shouldHaveCreatedNewPrintWriter() throws IOException {
            HttpServletResponse original = mock(HttpServletResponse.class);
            MutableHttpServletResponse actual;

            actual = MutableHttpServletResponse.wrap(original);

            assertNotNull(actual.getWriter());
        }
    }

    public static class WhenGettingInputStream {
        //TODO: redo this test, it doesn't really test much (this is Josh being snarky, not Fran)

        @Test
        public void shouldAlwaysCreateNewInstance() throws IOException {
            InputStream first, second;
            HttpServletResponse original = mock(HttpServletResponse.class);
            MutableHttpServletResponse response;

            response = MutableHttpServletResponse.wrap(original);

            first = response.getBufferedOutputAsInputStream();
            second = response.getBufferedOutputAsInputStream();

            InputStreamReader reader = new InputStreamReader(first);
            BufferedReader bufReader = new BufferedReader(reader);
            assertNull("seems kinda pointless, huh? - JL", bufReader.readLine());
            assertNotNull("first should not be null", first);
            assertNotNull("second should not be null", second);
            assertNotSame("should not be the same", first, second);
        }
    }

    @Ignore
    public static class WhenGettingWriter {

        @Test
        public void shouldWriteResponseMessageBody() throws IOException {
            String responseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<versions xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns3=\"http://docs.rackspacecloud.com/repose/versioning/v1.0\">"
                    + "<version xsi:type=\"ns3:ServiceVersionMapping\" pp-host-id=\"service-v1\" name=\"Service Version 1\" status=\"DEPRECATED\" id=\"v1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                    + "<ns2:link href=\"http://localhost:65000/v1/\" rel=\"self\"/>"
                    + "</version>"
                    + "<version xsi:type=\"ns3:ServiceVersionMapping\" pp-host-id=\"service-v2\" name=\"Service Version 2\" status=\"CURRENT\" id=\"v2\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                    + "<ns2:link href=\"http://localhost:65000/v2/\" rel=\"self\"/>"
                    + "</version>"
                    + "</versions>";

            HttpServletResponse original = mock(HttpServletResponse.class);

            ByteBuffer byteBuffer = new CyclicByteBuffer();
            ServletOutputStream outputStream = new ByteBufferServletOutputStream(byteBuffer);
            when(original.getOutputStream()).thenReturn(outputStream);

            MutableHttpServletResponse response = MutableHttpServletResponse.wrap(original);

            response.getWriter().write(responseBody);

            response.flushBuffer();

            InputStream inputStream = response.getBufferedOutputAsInputStream();


            InputStreamReader reader = new InputStreamReader(inputStream);
            BufferedReader bufReader = new BufferedReader(reader);

            assertNotNull(bufReader.readLine());
        }
    }
}
