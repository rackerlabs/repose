package org.openrepose.rnxp.servlet.http;

import java.io.IOException;
import java.io.OutputStream;
import org.openrepose.rnxp.http.io.control.CommittableHttpMessage;

/**
 *
 * @author zinic
 */
public class CommitAwareOutputStream extends OutputStream {

   private final CommittableHttpMessage committableHttpMessage;
   private final OutputStream delegateOutputStream;
   private boolean committed;

   public CommitAwareOutputStream(CommittableHttpMessage committableHttpMessage, OutputStream delegateOutputStream) {
      this.committableHttpMessage = committableHttpMessage;
      this.delegateOutputStream = delegateOutputStream;
      
      committed = false;
   }

   @Override
   public void write(int i) throws IOException {
      commit();
      
      delegateOutputStream.write(i);
   }
   
   @Override
   public void flush() throws IOException {
      commit();

      delegateOutputStream.flush();
   }

   private void commit() throws IOException {
      if (!committed) {
         committableHttpMessage.commitMessage();
         committed = true;
      }
   }
}
