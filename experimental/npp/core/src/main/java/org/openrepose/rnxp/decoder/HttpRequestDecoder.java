package org.openrepose.rnxp.decoder;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.decoder.partial.impl.HttpErrorPartial;
import org.openrepose.rnxp.decoder.partial.impl.RequestMethodPartial;
import org.openrepose.rnxp.decoder.partial.impl.RequestUriPartial;
import org.openrepose.rnxp.decoder.processor.HeaderProcessor;
import org.openrepose.rnxp.http.HttpMessageComponent;
import org.openrepose.rnxp.http.HttpMethod;

import static org.openrepose.rnxp.decoder.DecoderState.*;
import static org.openrepose.rnxp.decoder.AsciiCharacterConstant.*;

public class HttpRequestDecoder extends AbstractHttpMessageDecoder {

    private final HeaderProcessor fHeadHeaderProcessor;
    private HeaderProcessor currentHeaderProcessor;

    public HttpRequestDecoder() {
        fHeadHeaderProcessor = new HeaderProcessor() {

            @Override
            public HttpErrorPartial processHeader(String key, String value) {
                HttpErrorPartial messagePartial = null;

                if (key.equals("content-length") && getContentPresence() != ContentPresence.CHUNKED) {
                    try {
                        setContentLength(Long.parseLong(value));
                        setContentPresence(ContentPresence.STATIC_LENGTH);
                    } catch (NumberFormatException nfe) {
                        messagePartial = HttpErrors.malformedContentLength();
                    }
                } else if (key.equals("transfer-encoding") && value.equalsIgnoreCase("chunked")) {
                    setContentLength(-1);
                    setContentPresence(ContentPresence.CHUNKED);
                }

                return messagePartial;
            }

            @Override
            public void finishedReadingHeaders() {
                currentHeaderProcessor = null;
            }
        };

        currentHeaderProcessor = fHeadHeaderProcessor;
    }

    @Override
    protected DecoderState initialState() {
        return READ_SC_PARSE_METHOD;
    }

    @Override
    protected Object httpDecode(ChannelHandlerContext chc, Channel chnl, ChannelBuffer socketBuffer) throws Exception {
        try {
            switch (getDecoderState()) {
                case READ_SC_PARSE_METHOD:
                    return readSingleCharacterParsableMethod(socketBuffer);

                case READ_MC_PARSE_METHOD:
                    return readMultiCharacterParsableMethod(socketBuffer);

                case READ_URI:
                    return readRequestURI(socketBuffer);

                case READ_VERSION:
                    return readRequestVersion(socketBuffer);

                case READ_HEADER_KEY:
                    return readHeaderKey(socketBuffer, currentHeaderProcessor);

                case READ_HEADER_VALUE:
                    return readHeaderValue(socketBuffer, currentHeaderProcessor);

                case READ_CONTENT:
                    return readContent(socketBuffer);

                case READ_CHUNK_LENGTH:
                    return readContentChunkLength(socketBuffer);

                case READ_CONTENT_CHUNKED:
                    return readContentChunked(socketBuffer);
                case STOP:
            }
        } catch (IndexOutOfBoundsException boundsException) {
            // TODO:Review - Log this?

            return HttpErrors.bufferOverflow(getDecoderState());
        }

        return null;
    }

    private HttpMessagePartial readSingleCharacterParsableMethod(ChannelBuffer buffer) {
        final char nextCharacter = (char) buffer.readByte();

        for (HttpMethod method : HttpMethod.SC_PARSE_METHODS) {
            if (nextCharacter == method.getMatcherFragment()[0]) {
                final RequestMethodPartial methodPartial = new RequestMethodPartial(HttpMessageComponent.REQUEST_METHOD, method);

                // Skip the remaining method length plus the following whitespace
                skipFollowingBytes(method.getSkipLength() + 1);
                setDecoderState(READ_URI);

                return methodPartial;
            }
        }

        setDecoderState(READ_MC_PARSE_METHOD);

        return null;
    }

    private HttpMessagePartial readMultiCharacterParsableMethod(ChannelBuffer buffer) {
        final char nextCharacter = (char) buffer.readByte();
        HttpMessagePartial messagePartial = HttpErrors.methodNotImplemented();

        for (HttpMethod method : HttpMethod.MC_PARSE_METHODS) {
            if (nextCharacter == method.getMatcherFragment()[1]) {
                messagePartial = new RequestMethodPartial(HttpMessageComponent.REQUEST_METHOD, method);

                // Skip the remaining method length plus the following whitespace
                skipFollowingBytes(method.getSkipLength() + 1);
                setDecoderState(READ_URI);

                break;
            }
        }

        return messagePartial;
    }

    private HttpMessagePartial readRequestURI(ChannelBuffer socketBuffer) {
        final ControlCharacter readChar = readUntilCaseSensitive(socketBuffer, SPACE);

        if (readChar != null) {
            // TODO: Validate
            final RequestUriPartial uriPartial = new RequestUriPartial(HttpMessageComponent.REQUEST_URI, flushCharacterBuffer());
            setDecoderState(READ_VERSION);

            return uriPartial;
        }

        return null;
    }

    private HttpMessagePartial readRequestVersion(ChannelBuffer socketBuffer) {
        HttpMessagePartial messagePartial = readHttpVersion(socketBuffer, CARRIAGE_RETURN);

        if (messagePartial != null) {
            // Set ourselves up to process head headers
            currentHeaderProcessor = fHeadHeaderProcessor;
            setDecoderState(READ_HEADER_KEY);
        }

        return messagePartial;
    }
}