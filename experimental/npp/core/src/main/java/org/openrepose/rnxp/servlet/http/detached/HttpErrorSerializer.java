package org.openrepose.rnxp.servlet.http.detached;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.openrepose.rnxp.decoder.partial.impl.HttpErrorPartial;

import org.openrepose.rnxp.http.util.StringCharsetEncoder;

import static org.jboss.netty.buffer.ChannelBuffers.*;
import static org.openrepose.rnxp.http.io.control.HttpControlSequence.*;

/**
 *
 * @author zinic
 */
public class HttpErrorSerializer {

    private static final StringCharsetEncoder ASCII_ENCODER = StringCharsetEncoder.asciiEncoder();
    
    private final HttpErrorPartial errorPartial;
    
    public HttpErrorSerializer(HttpErrorPartial errorPartial) {
        this.errorPartial = errorPartial;
    }

    public ChannelFuture writeTo(Channel channel) {
        final ChannelBuffer buffer = buffer(512); //TODO:Optimization - Trim this to better fit the generated status line
        
        buffer.writeBytes(HTTP_VERSION.getBytes());
        buffer.writeBytes(SPACE.getBytes());
        buffer.writeBytes(ASCII_ENCODER.encode(errorPartial.getStatusCode().intValue()));
        buffer.writeBytes(SPACE.getBytes());
        buffer.writeBytes(LINE_END.getBytes());
        buffer.writeBytes(LINE_END.getBytes());
        
        return channel.write(buffer);
    }
}
