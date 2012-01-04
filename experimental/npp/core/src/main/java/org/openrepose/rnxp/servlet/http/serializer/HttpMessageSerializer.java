package org.openrepose.rnxp.servlet.http.serializer;

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author zinic
 */
public interface HttpMessageSerializer {

   void writeTo(OutputStream os) throws IOException;

   int read();
}
