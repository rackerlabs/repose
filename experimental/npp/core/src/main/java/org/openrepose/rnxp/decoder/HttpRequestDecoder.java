package org.openrepose.rnxp.decoder;


import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import org.openrepose.rnxp.decoder.partial.ContentMessagePartial;
import org.openrepose.rnxp.decoder.partial.EmptyHttpMessagePartial;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.decoder.partial.impl.HeaderPartial;
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
        currentHeaderProcessor = fHeadHeaderProcessor = new HeaderProcessor() {

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
        };
    }

    @Override
    protected DecoderState initialState() {
        return READ_SC_PARSE_METHOD;
    }

    @Override
    protected Object httpDecode(ChannelHandlerContext chc, Channel chnl, ChannelBuffer buffer) throws Exception {
        try {
            switch (getDecoderState()) {
                case READ_SC_PARSE_METHOD:
                    return readSingleCharacterParsableMethod(buffer);

                case READ_MC_PARSE_METHOD:
                    return readMultiCharacterParsableMethod(buffer);

                case READ_URI:
                    return readRequestURI(buffer);

                case READ_VERSION:
                    return readRequestVersion(buffer);

                case READ_HEADER_KEY:
                    return readHeaderKey(buffer);

                case READ_HEADER_VALUE:
                    return readHeaderValue(buffer, currentHeaderProcessor);

                case READ_CONTENT:
                    return readContent(buffer);

                case READ_CHUNK_LENGTH:
                    return readContentChunkLength(buffer);

                case READ_CONTENT_CHUNKED:
                    return readContentChunked(buffer);

                case STREAM_REMAINING:
                case READ_END:
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

            // Skip the next bit of whitespace
            skipFollowingBytes(1);

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

    private HttpMessagePartial readHeaderKey(ChannelBuffer buffer) {
        final ControlCharacter controlCharacter = readUntilCaseInsensitive(buffer, COLON, CARRIAGE_RETURN);
        HttpMessagePartial messagePartial = null;

        if (controlCharacter != null) {
            switch (controlCharacter.getCharacterConstant()) {
                case COLON:
                    // The header key must have data
                    if (characterBufferReadable()) {
                        flipBuffers();

                        setDecoderState(READ_HEADER_VALUE);
                    } else {
                        messagePartial = HttpErrors.badHeaderKey();
                    }

                    break;

                case CARRIAGE_RETURN:
                    // Skip the expected LF
                    skipFollowingBytes(1);

                    switch (getContentPresence()) {
                        case CHUNKED:
                            messagePartial = new EmptyHttpMessagePartial(HttpMessageComponent.CONTENT_START);
                            setDecoderState(READ_CHUNK_LENGTH);
                            break;

                        case STATIC_LENGTH:
                            messagePartial = new EmptyHttpMessagePartial(HttpMessageComponent.CONTENT_START);
                            setDecoderState(READ_CONTENT);
                            break;

                        default:
                            messagePartial = new EmptyHttpMessagePartial(HttpMessageComponent.MESSAGE_END_NO_CONTENT);
                            setDecoderState(READ_END);
                    }
            }
        }

        return messagePartial;

    }

    private HttpMessagePartial readHeaderValue(ChannelBuffer buffer, HeaderProcessor headerProcessor) {
        HttpMessagePartial messagePartial = null;

        if (readUntilCaseSensitive(buffer, CARRIAGE_RETURN) != null) {
            // Skip the expected LF
            skipFollowingBytes(1);

            // Dump the buffers to encoded strings
            final String headerValue = flushCharacterBuffer();
            flipBuffers();            
            
            final String headerKey = flushCharacterBuffer();

            if (headerProcessor != null) {
                messagePartial = headerProcessor.processHeader(headerKey, headerValue);
            }

            setDecoderState(READ_HEADER_KEY);

            if (messagePartial == null) {
                messagePartial = new HeaderPartial(HttpMessageComponent.ENTITY_HEADER, headerKey, headerValue);
            }
        }

        return messagePartial;
    }

    private HttpMessagePartial readContent(ChannelBuffer buffer) {
        HttpMessagePartial messagePartial = null;

        final byte readByte = buffer.readByte();
        contentRead();

        if (getContentLength() > 0) {
            messagePartial = new ContentMessagePartial(readByte);
        } else {
            messagePartial = new ContentMessagePartial(HttpMessageComponent.MESSAGE_END_WITH_CONTENT, readByte);
            setDecoderState(READ_END);
        }

        return messagePartial;
    }

    private HttpMessagePartial readContentChunkLength(ChannelBuffer buffer) {
        HttpMessagePartial messagePartial = null;

        if (readUntilCaseInsensitive(buffer, CARRIAGE_RETURN) != null) {
            // Skip the expected LF
            skipFollowingBytes(1);

            parseChunkLength(flushCharacterBuffer());
        }

        return messagePartial;
    }

    private HttpMessagePartial parseChunkLength(String hexEncodedChunkLength) {
        HttpMessagePartial messagePartial = null;

        try {
            updateChunkLength(Long.parseLong(hexEncodedChunkLength, 16));
        } catch (NumberFormatException nfe) {
            messagePartial = HttpErrors.malformedChunkLength();
        }

        return messagePartial;
    }

    private void updateChunkLength(long newChunkLength) {
        if (newChunkLength < 0) {
            //TODO: Errorcase
        } else if (newChunkLength == 0) {
            currentHeaderProcessor = null;
            setContentPresence(ContentPresence.NO_CONTENT);

            // We're done reading content
            setDecoderState(READ_HEADER_KEY);
        } else {
            setContentLength(newChunkLength);
            setDecoderState(READ_CONTENT_CHUNKED);
        }
    }

    private HttpMessagePartial readContentChunked(ChannelBuffer buffer) {
        HttpMessagePartial messagePartial = null;

        final byte nextByte = buffer.readByte();
        contentRead(1);

        if (CARRIAGE_RETURN.matches((char) nextByte)) {
            // Skip expected LF
            skipFollowingBytes(1);

            setDecoderState(READ_CHUNK_LENGTH);
        } else {
            messagePartial = new ContentMessagePartial(nextByte);
        }

        return messagePartial;
    }
}