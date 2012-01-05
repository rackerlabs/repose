package org.openrepose.rnxp.servlet.http.live;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.decoder.partial.impl.HeaderPartial;
import org.openrepose.rnxp.decoder.partial.impl.StatusCodePartial;
import org.openrepose.rnxp.http.HttpMessageComponent;
import org.openrepose.rnxp.http.HttpMessageComponentOrder;
import org.openrepose.rnxp.http.io.control.HttpConnectionController;
import org.openrepose.rnxp.servlet.http.CommitAwareOutputStream;
import org.openrepose.rnxp.servlet.http.ServletOutputStreamWrapper;
import org.openrepose.rnxp.servlet.http.serializer.ResponseHeadSerializer;

/**
 *
 * @author zinic
 */
public class LiveHttpServletResponse extends AbstractHttpServletResponse implements UpdatableHttpServletResponse {

   private final Map<String, List<String>> headerMap;
   private final ServletOutputStream servletOutputStream;
   private final OutputStream channelOutputStream;
   private boolean committed;
   private HttpStatusCode statusCode;

   public LiveHttpServletResponse(HttpConnectionController updateController) {
      super(updateController, HttpMessageComponentOrder.responseOrderInstance());

      headerMap = new HashMap<String, List<String>>();

      committed = false;

      channelOutputStream = updateController.getCoordinator().getClientOutputStream();
      servletOutputStream = new ServletOutputStreamWrapper(new CommitAwareOutputStream(this, channelOutputStream));
   }

   public void delegateStreamToResponse(HttpServletResponse response) throws IOException {
      RawInputStreamReader.instance().copyTo(getPushInputStream(), response.getOutputStream());
   }

   @Override
   public ServletOutputStream getOutputStream() throws IOException {
      return servletOutputStream;
   }

   @Override
   public int getStatus() {
      return statusCode.intValue();
   }

   @Override
   public void setStatus(int sc) {
      statusCode = HttpStatusCode.fromInt(sc);
   }

   @Override
   public boolean isCommitted() {
      return committed;
   }

   @Override
   public synchronized void commitMessage() throws IOException {
      if (!committed) {
         final ResponseHeadSerializer serializer = new ResponseHeadSerializer(this);

         serializer.writeTo(channelOutputStream);
         committed = true;
      }
   }

   @Override
   public void flushBuffer() throws IOException {
      final InputStream inputStream = getPushInputStream();
      RawInputStreamReader.instance().copyTo(inputStream, servletOutputStream);

      servletOutputStream.flush();
   }

   @Override
   protected void mergeWithPartial(HttpMessagePartial partial) {
      switch (partial.getHttpMessageComponent()) {
         case RESPONSE_STATUS_CODE:
            statusCode = ((StatusCodePartial) partial).getStatusCode();
            break;

         case HEADER:
            addHeaderValues(((HeaderPartial) partial).getHeaderKey(), ((HeaderPartial) partial).getHeaderValue());
            break;
      }
   }

   @Override
   public String getHeader(String name) {
      loadComponent(HttpMessageComponent.HEADER);

      final List<String> headerValues = headerMap.get(name);
      return headerValues != null && headerValues.size() > 0 ? headerValues.get(0) : null;
   }

   @Override
   public Collection<String> getHeaderNames() {
      return Collections.unmodifiableCollection(headerMap.keySet());
   }

   @Override
   public Collection<String> getHeaders(String name) {
      return Collections.unmodifiableCollection(headerMap.get(name));
   }

   @Override
   public void addHeader(String name, String value) {
      addHeaderValues(name, value);
   }

   private List<String> newHeaderList(String headerKey) {
      final List<String> newList = new LinkedList<String>();
      headerMap.put(headerKey, newList);

      return newList;
   }

   private List<String> getHeaderList(String headerKey) {
      final List<String> list = headerMap.get(headerKey);

      return list != null ? list : newHeaderList(headerKey);
   }

   public void addHeaderValues(String headerKey, String... values) {
      final List<String> headerList = getHeaderList(headerKey);

      headerList.addAll(Arrays.asList(values));
   }

   public void putHeaderValues(String headerKey, String... values) {
      final List<String> headerList = getHeaderList(headerKey);
      headerList.clear();

      headerList.addAll(Arrays.asList(values));
   }
}
