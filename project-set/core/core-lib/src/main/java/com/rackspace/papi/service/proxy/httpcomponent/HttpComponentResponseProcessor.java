package com.rackspace.papi.service.proxy.httpcomponent;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.http.HttpServletResponse;
import com.rackspace.papi.http.proxy.common.AbstractResponseProcessor;
import org.apache.http.util.EntityUtils;

public class HttpComponentResponseProcessor extends AbstractResponseProcessor {

   private final HttpResponse httpResponse;

   public HttpComponentResponseProcessor(HttpResponse httpResponse, HttpServletResponse response, HttpComponentResponseCodeProcessor responseCode) {
      super(response, responseCode.getCode());
      this.httpResponse = httpResponse;
   }

   @Override
   protected void setResponseHeaders() throws IOException {
      for (Header header : httpResponse.getAllHeaders()) {
         addHeader(header.getName(), header.getValue());
      }
   }

   @Override
   protected void setResponseBody() throws IOException {
      HttpEntity entity = httpResponse.getEntity();
      if (entity != null) {
         if (getResponse() instanceof MutableHttpServletResponse) {
            MutableHttpServletResponse mutableResponse = (MutableHttpServletResponse)getResponse();
            mutableResponse.setInputStream(new HttpComponentInputStream(entity));
         } else {
            final OutputStream clientOut = getResponse().getOutputStream();
            entity.writeTo(clientOut);
            clientOut.flush();
            EntityUtils.consume(entity);
         }
      }

   }

   @Override
   protected String getResponseHeaderValue(String headerName) throws com.rackspace.papi.http.proxy.HttpException {
      final Header[] locationHeader = httpResponse.getHeaders(headerName);
      if (locationHeader == null || locationHeader.length == 0) {
         throw new com.rackspace.papi.http.proxy.HttpException("Expected header was not found in response: " + headerName + " (Response Code: " + getResponseCode() + ")");
      }

      final String locationValue = locationHeader[0].getValue();
      if (locationValue == null) {
         throw new com.rackspace.papi.http.proxy.HttpException("Expected header was not found in response: " + headerName + " (Response Code: " + getResponseCode() + ")");
      }

      return locationValue;
   }
}
