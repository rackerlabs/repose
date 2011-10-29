package org.openrepose.rnxp.decoder;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import org.openrepose.rnxp.decoder.processor.HeaderProcessor;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.http.HttpMessageComponent;
import org.openrepose.rnxp.http.HttpMethod;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import org.openrepose.rnxp.decoder.partial.EmptyHttpMessagePartial;
import org.openrepose.rnxp.decoder.partial.ContentMessagePartial;
import org.openrepose.rnxp.decoder.partial.impl.HeaderPartial;
import org.openrepose.rnxp.decoder.partial.impl.HttpErrorPartial;
import org.openrepose.rnxp.decoder.partial.impl.HttpVersionPartial;
import org.openrepose.rnxp.decoder.partial.impl.RequestMethodPartial;
import org.openrepose.rnxp.decoder.partial.impl.RequestUriPartial;
import org.openrepose.rnxp.decoder.partial.impl.StatusCodePartial;
import static org.openrepose.rnxp.decoder.DecoderState.*;
import static org.openrepose.rnxp.decoder.AsciiCharacterConstant.*;
import static org.jboss.netty.buffer.ChannelBuffers.*;

public class HttpResponseDecoder extends AbstractHttpMessageDecoder {

    @Override
    protected DecoderState initialState() {
        return READ_VERSION;
    }

    @Override
    protected Object httpDecode(ChannelHandlerContext chc, Channel chnl, ChannelBuffer socketBuffer) throws Exception {
        switch (getDecoderState()) {
            case READ_VERSION:
                return readRequestVersion(socketBuffer);
                
            case READ_STATUS_CODE:
                return readStatusCode(socketBuffer);

        }
        
        return null;
    }

    private HttpMessagePartial readRequestVersion(ChannelBuffer socketBuffer) {
        HttpMessagePartial messagePartial = readHttpVersion(socketBuffer, CARRIAGE_RETURN);

        if (messagePartial != null) {
            setDecoderState(READ_STATUS_CODE);
        }

        return messagePartial;
    }
    
    private HttpMessagePartial readStatusCode(ChannelBuffer socketBuffer) {
        HttpMessagePartial messagePartial = null;
        
        if (readUntilCaseInsensitive(socketBuffer, SPACE) != null) {
            final String versionString = flushCharacterBuffer();
            HttpStatusCode code;
            
            try {
                // TODO:Review - Is this a good idea? Should we just past the code as is and adhere to validation rules in the RFC?
                code = HttpStatusCode.fromInt(Integer.parseInt(versionString));
                
                if (code != HttpStatusCode.UNSUPPORTED_RESPONSE_CODE) {
                    messagePartial = new StatusCodePartial(code);
                } else {
                    messagePartial = HttpErrors.badStatusCode();
                }
            }catch(NumberFormatException exception) {
                return HttpErrors.badStatusCode();
            }
        }
        
        return messagePartial;
    }
}