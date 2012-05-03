package org.openrepose.rnxp.servlet.http.detached;

import org.openrepose.rnxp.servlet.http.serializer.ResponseHeadSerializer;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import org.openrepose.rnxp.http.io.control.HttpConnectionController;
import org.openrepose.rnxp.servlet.http.ServletOutputStreamWrapper;

/**
 *
 * @author zinic
 */
public class ClientHttpServletResponse extends AbstractHttpServletResponse {

   private final Map<String, List<String>> headerMap;
   private final ServletOutputStreamWrapper<OutputStream> outputStream;
   private HttpStatusCode statusCode;
   private boolean committed;

   public ClientHttpServletResponse(HttpConnectionController connectionController) throws IOException {
      outputStream = new ServletOutputStreamWrapper<OutputStream>(connectionController.getCoordinator().getClientOutputStream());

      statusCode = HttpStatusCode.NOT_FOUND;
      headerMap = new HashMap<String, List<String>>();
      committed = false;
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

   private void commit() throws IOException {
      if (!committed) {
         final ResponseHeadSerializer serializer = new ResponseHeadSerializer(this);
         int read;
         
         while ((read = serializer.read()) != -1) {
            outputStream.write(read);
         }

         outputStream.flush();
         committed = true;
      }
   }

   @Override
   public void flushBuffer() throws IOException {
      commit();
      
      outputStream.flush();
   }

   @Override
   public ServletOutputStream getOutputStream() throws IOException {
      commit();
      
      return outputStream;
   }

   @Override
   public String getHeader(String name) {
      final List<String> headerValues = headerMap.get(name);
      return headerValues != null && headerValues.size() > 0 ? headerValues.get(0) : null;
   }

   @Override
   public Collection<String> getHeaderNames() {
      return Collections.unmodifiableCollection(headerMap.keySet());
   }

   @Override
   public Collection<String> getHeaders(String name) {
      final List<String> headerValues = headerMap.get(name);
      return headerValues != null && headerValues.size() > 0 ? Collections.unmodifiableCollection(headerValues) : null;
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

   @Override
   public void addHeader(String headerKey, String value) {
      final List<String> headerList = getHeaderList(headerKey);

      headerList.add(value);
   }

   @Override
   public void setHeader(String headerKey, String value) {
      final List<String> headerList = getHeaderList(headerKey);
      headerList.clear();

      headerList.add(value);
   }
}
