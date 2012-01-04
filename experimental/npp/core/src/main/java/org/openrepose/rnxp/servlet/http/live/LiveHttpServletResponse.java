package org.openrepose.rnxp.servlet.http.live;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.decoder.partial.impl.StatusCodePartial;
import org.openrepose.rnxp.http.HttpMessageComponentOrder;
import org.openrepose.rnxp.http.io.control.HttpConnectionController;
import org.openrepose.rnxp.servlet.http.ServletOutputStreamWrapper;
import org.openrepose.rnxp.servlet.http.serializer.ResponseHeadSerializer;

/**
 *
 * @author zinic
 */
public class LiveHttpServletResponse extends AbstractHttpServletResponse implements UpdatableHttpServletResponse {

   private final ServletOutputStream outputStream;
   private boolean committed;
   private HttpStatusCode statusCode;

   public LiveHttpServletResponse(HttpConnectionController updateController) {
      super(updateController, HttpMessageComponentOrder.responseOrderInstance());

      committed = false;
      outputStream = new ServletOutputStreamWrapper(updateController.getCoordinator().getClientOutputStream());
   }
   
   // TODO: Connect this sucker to the right spot - man is this ugly
   public void delegateStreaToResponse(HttpServletResponse response) throws IOException {
      RawInputStreamReader.instance().copyTo(getPushInputStream(), response.getOutputStream());
   }

   @Override
   public ServletOutputStream getOutputStream() throws IOException {
      return outputStream;
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
         
         serializer.writeTo(outputStream);
         outputStream.flush();
         
         committed = true;
      }
   }

   @Override
   public void flushBuffer() throws IOException {
      commit();

      final InputStream inputStream = getPushInputStream();

      RawInputStreamReader.instance().copyTo(inputStream, outputStream);

      outputStream.flush();
   }

   @Override
   protected void mergeWithPartial(HttpMessagePartial partial) {
      switch (partial.getHttpMessageComponent()) {
         case RESPONSE_STATUS_CODE:
            statusCode = ((StatusCodePartial) partial).getStatusCode();
            break;
      }
   }
}
