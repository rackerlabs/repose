package org.openrepose.commons.utils.http;

import org.apache.http.Header;

import java.io.InputStream;

/**
 *
 * @author Dan Daley
 */
public class ServiceClientResponse {

   private final InputStream data;
   private final int statusCode;
   private final Header[] headers;

   public ServiceClientResponse(int code, InputStream data) {
      this.statusCode = code;
      this.headers = null;
      this.data = data;
   }

   public ServiceClientResponse(int code, Header[] headers, InputStream data) {
       this.statusCode = code;
       this.headers = headers;
       this.data = data;
   }

   public InputStream getData() {
      return data;
   }

   public Header[] getHeaders() { return headers; }

   public int getStatus() {
      return statusCode;
   }
}
