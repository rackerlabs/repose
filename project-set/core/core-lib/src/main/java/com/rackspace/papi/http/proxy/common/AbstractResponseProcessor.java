package com.rackspace.papi.http.proxy.common;

import com.rackspace.papi.commons.util.StringUtilities;
import static com.rackspace.papi.http.Headers.CONTENT_LENGTH;
import static com.rackspace.papi.http.Headers.LOCATION;
import com.rackspace.papi.http.proxy.httpclient.HttpResponseCodeProcessor;
import java.io.IOException;
import java.util.Arrays;
import java.util.TreeSet;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpException;

public abstract class AbstractResponseProcessor {
   private final static String[] EXCLUDE_HEADERS = { "connection", "transfer-encoding", "server" };
   private final static TreeSet<String> EXCLUDE_HEADERS_SET = new TreeSet<String>(Arrays.asList(EXCLUDE_HEADERS));
   
   private final HttpServletResponse response;
   private final HttpResponseCodeProcessor responseCode;
   
   public AbstractResponseProcessor(HttpServletResponse response, int status) {
      this.response = response;
      this.responseCode = new HttpResponseCodeProcessor(status);
   }

   public HttpServletResponse getResponse() {
      return response;
   }
   
   public HttpResponseCodeProcessor getResponseCode() {
      return responseCode;
   }
   
   protected void sendRedirect(String url) throws IOException {
      response.sendRedirect(url);
   }
   
   protected void setStatus() {
      response.setStatus(responseCode.getCode());
   }
   
   protected void addHeader(String name, String value) {
      if (!EXCLUDE_HEADERS_SET.contains(name.toLowerCase())) {
         response.setHeader(name, value);
      }
   }
   
   protected abstract void setResponseHeaders() throws IOException;
   protected abstract void setResponseBody() throws IOException;
   protected abstract String getResponseHeaderValue(String name) throws HttpException;

   private String translateRedirectUrl(String proxiedRedirectUrl, String proxiedHostUrl, String requestHostPath) {
      if (StringUtilities.isEmpty(proxiedRedirectUrl)) {
         return requestHostPath;
      }
      return proxiedRedirectUrl.replace(proxiedHostUrl, requestHostPath);
   }

   /**
    *
    * @param proxiedHostUrl - host:port/contextPath to the origin service
    * @param requestHostPath - host:port/contextPath from the client request
    * @throws HttpException
    * @throws IOException
    */
   public void sendTranslatedRedirect(String proxiedHostUrl, String requestHostPath) throws HttpException, IOException {
      final String proxiedRedirectUrl = getResponseHeaderValue(LOCATION.name());
      final String translatedRedirectUrl = translateRedirectUrl(proxiedRedirectUrl, proxiedHostUrl, requestHostPath);

      sendRedirect(translatedRedirectUrl);
   }
   
   public void process() throws IOException {
      setStatus();
      
      if (getResponseCode().isNotModified()) {
         // http://www.ics.uci.edu/pub/ietf/http/rfc1945.html#Code304
         getResponse().setIntHeader(CONTENT_LENGTH.toString(), 0);
      } else {
         setResponseHeaders();
         setResponseBody();
      }
   }
}
