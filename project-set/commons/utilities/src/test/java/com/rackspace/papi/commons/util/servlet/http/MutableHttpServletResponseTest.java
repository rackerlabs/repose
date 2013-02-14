package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.http.HttpDate;
import com.rackspace.papi.commons.util.io.ByteBufferServletOutputStream;
import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;
import com.rackspace.papi.commons.util.io.buffer.CyclicByteBuffer;
import java.io.*;
import java.util.Date;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.*;
import org.junit.Before;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class MutableHttpServletResponseTest {

  public static class WhenModifyingHeaders {
    private HttpServletRequest request;
    private HttpServletResponse response;
    private MutableHttpServletResponse mutableResponse;
    @Before
    public void setup() {
      request = mock(HttpServletRequest.class);
      response = mock(HttpServletResponse.class);
      mutableResponse = MutableHttpServletResponse.wrap(request, response);
    }
    
    @Test
    public void shouldSetIntHeader() {

      int actual = 10;
      int expected = 10;
      
      mutableResponse.setIntHeader("intHeader", actual);
      assertNotNull(mutableResponse.getHeader("intHeader"));
      assertEquals(10, Integer.parseInt(mutableResponse.getHeader("intHeader")));
    }
    
    @Test
    public void shouldSetDateHeader() {
      long expected = new Date().getTime();
      HttpDate httpDate = new HttpDate(new Date(expected));
      
      mutableResponse.setDateHeader("date", expected);
      assertNotNull(mutableResponse.getHeader("date"));
      assertEquals(httpDate.toRFC1123(), mutableResponse.getHeader("date"));
    }
    
  }

  public static class WhenCreatingNewInstances {

    @Test
    public void shouldPassReferenceThroughIfIsWrapperInstance() {
      MutableHttpServletResponse original = MutableHttpServletResponse.wrap(mock(HttpServletRequest.class), mock(HttpServletResponse.class));
      MutableHttpServletResponse actual;

      actual = MutableHttpServletResponse.wrap(mock(HttpServletRequest.class), original);

      assertSame(original, actual);
    }

    @Test
    public void shouldCreateNewInstanceIfIsNotWrapperInstance() {
      HttpServletResponse original = mock(HttpServletResponse.class);
      MutableHttpServletResponse actual;

      actual = MutableHttpServletResponse.wrap(mock(HttpServletRequest.class), original);

      assertNotSame(original, actual);
    }

    @Test
    public void shouldHaveInitializedBuffer() throws IOException {
      HttpServletResponse original = mock(HttpServletResponse.class);
      MutableHttpServletResponse actual;

      actual = MutableHttpServletResponse.wrap(mock(HttpServletRequest.class), original);
      actual.getOutputStream();

      assertEquals(1024, actual.getBufferSize());
    }

    @Test
    public void shouldHaveCreatedNewOutputStream() throws IOException {
      HttpServletResponse original = mock(HttpServletResponse.class);
      MutableHttpServletResponse actual;

      actual = MutableHttpServletResponse.wrap(mock(HttpServletRequest.class), original);

      assertTrue(actual.getOutputStream() instanceof ByteBufferServletOutputStream);
    }

    @Test
    public void shouldHaveCreatedNewPrintWriter() throws IOException {
      HttpServletResponse original = mock(HttpServletResponse.class);
      MutableHttpServletResponse actual;

      actual = MutableHttpServletResponse.wrap(mock(HttpServletRequest.class), original);

      assertNotNull(actual.getWriter());
    }

    @Test
    public void shouldGetOutputQueue() {
      HttpServletResponse response = mock(HttpServletResponse.class);
      HttpServletRequest request = mock(HttpServletRequest.class);
      MutableHttpServletResponse actual;

      actual = MutableHttpServletResponse.wrap(request, response);
      verify(request).getAttribute(eq("repose.response.output.queue"));
      verify(request).setAttribute(eq("repose.response.output.queue"), anyObject());

    }
  }

  public static class WhenGettingAndSettingInputStream {
    //TODO: redo this test, it doesn't really test much (this is Josh being snarky, not Fran)

    @Test
    public void shouldAlwaysCreateNewInstance() throws IOException {
      InputStream first, second;
      HttpServletResponse original = mock(HttpServletResponse.class);
      MutableHttpServletResponse response;

      response = MutableHttpServletResponse.wrap(mock(HttpServletRequest.class), original);

      first = response.getBufferedOutputAsInputStream();
      second = response.getBufferedOutputAsInputStream();

      InputStreamReader reader = new InputStreamReader(first);
      BufferedReader bufReader = new BufferedReader(reader);
      assertNull("seems kinda pointless, huh? - JL", bufReader.readLine());
      assertNotNull("first should not be null", first);
      assertNotNull("second should not be null", second);
      assertNotSame("should not be the same", first, second);
    }

    @Test
    public void shouldResetBuffer() {
      HttpServletResponse response = mock(HttpServletResponse.class);
      HttpServletRequest request = mock(HttpServletRequest.class);
      MutableHttpServletResponse actual;

      actual = MutableHttpServletResponse.wrap(request, response);

      actual.resetBuffer();
      verify(response).resetBuffer();
    }

    @Test
    public void shouldSetInputStreamInRequest() throws IOException {
      HttpServletResponse response = mock(HttpServletResponse.class);
      HttpServletRequest request = mock(HttpServletRequest.class);
      InputStream in = mock(InputStream.class);
      MutableHttpServletResponse actual;

      actual = MutableHttpServletResponse.wrap(request, response);
      actual.setInputStream(in);
      verify(request).setAttribute(eq("repose.response.input.stream"), eq(in));

    }
  }

  public static class WhenWritingAndReadingInputStreams {
    //TODO: redo this test, it doesn't really test much (this is Josh being snarky, not Fran)

    @Test
    public void should() throws IOException {
      OutputStream out;
      InputStream in;
      HttpServletResponse original = mock(HttpServletResponse.class);
      MutableHttpServletResponse response;

      response = MutableHttpServletResponse.wrap(mock(HttpServletRequest.class), original);

      out = response.getOutputStream();
      in = response.getBufferedOutputAsInputStream();
      final int dataLen = 10;
      byte[] data = new byte[dataLen];

      assertNotNull("input stream should not be null", in);
      assertNotNull("output stream should not be null", out);
      assertEquals("available should be zero", 0, in.available());

      out.write(data);

      assertEquals("available should be " + dataLen, dataLen, in.available());
      assertEquals("Response size should be " + dataLen, dataLen, response.getResponseSize());
      assertEquals("buffer size", 1024, response.getBufferSize());

      int readLen = dataLen / 2;
      byte[] read = new byte[readLen];
      in.read(read);

      final int expected = dataLen - readLen;

      assertEquals("available should be " + expected, expected, in.available());

      out.write(read);
      assertEquals("available should be " + dataLen, dataLen, in.available());
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

      MutableHttpServletResponse response = MutableHttpServletResponse.wrap(mock(HttpServletRequest.class), original);

      response.getWriter().write(responseBody);

      response.flushBuffer();

      InputStream inputStream = response.getBufferedOutputAsInputStream();


      InputStreamReader reader = new InputStreamReader(inputStream);
      BufferedReader bufReader = new BufferedReader(reader);

      assertNotNull(bufReader.readLine());
    }
  }
}
