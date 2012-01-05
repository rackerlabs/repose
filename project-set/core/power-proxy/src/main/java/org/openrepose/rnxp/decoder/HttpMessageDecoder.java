package org.openrepose.rnxp.decoder;

import org.jboss.netty.buffer.ChannelBuffer;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;

/**
 *
 * @author zinic
 */
public interface HttpMessageDecoder {

    HttpMessagePartial decode(ChannelBuffer cb);
}
