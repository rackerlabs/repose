package org.openrepose.rnxp.servlet.http.live;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.ServletOutputStream;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.decoder.partial.impl.StatusCodePartial;
import org.openrepose.rnxp.http.io.control.HttpConnectionController;
import org.openrepose.rnxp.http.io.control.HttpMessageSerializer;
import org.openrepose.rnxp.servlet.http.ServletOutputStreamWrapper;
import org.openrepose.rnxp.servlet.http.detached.ResponseHeadSerializer;

/**
 *
 * @author zinic
 */
public class LiveHttpServletResponse extends AbstractHttpServletResponse implements UpdatableHttpServletResponse {

   private final ServletOutputStream outputStream;
   private boolean committed;
   private HttpStatusCode statusCode;

   public LiveHttpServletResponse(HttpConnectionController updateController) {
      setUpdateController(updateController);

      outputStream = new ServletOutputStreamWrapper(updateController.getCoordinator().getClientOutputStream());
   }

   @Override
   public ServletOutputStream getOutputStream() throws IOException {
      commitMessage();

      return outputStream;
   }

   @Override
   public void setStatus(int sc) {
      statusCode = HttpStatusCode.fromInt(sc);
   }

   @Override
   public synchronized void commitMessage() throws IOException {
      if (committed) {
         throw new IllegalStateException("Response has already been committed");
      }

      final OutputStream os = getOutputStream();

      // This commits the message - opening the output stream is serious business
      final HttpMessageSerializer serializer = new ResponseHeadSerializer(this);
      int read;

      while ((read = serializer.read()) != -1) {
         os.write(read);
      }

      os.flush();
      committed = true;
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
