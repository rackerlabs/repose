package org.openrepose.rnxp.decoder;

import org.openrepose.rnxp.decoder.processor.HeaderProcessor;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import java.io.StringWriter;
import org.openrepose.rnxp.http.HttpMessageComponent;
import org.openrepose.rnxp.http.HttpMethod;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import org.openrepose.rnxp.decoder.partial.EmptyHttpMessagePartial;
import org.openrepose.rnxp.decoder.partial.ContentMessagePartial;
import org.openrepose.rnxp.decoder.partial.impl.HeaderPartial;
import org.openrepose.rnxp.decoder.partial.impl.HttpErrorPartial;
import org.openrepose.rnxp.decoder.partial.impl.HttpVersionPartial;
import org.openrepose.rnxp.decoder.partial.impl.RequestMethodPartial;
import org.openrepose.rnxp.decoder.partial.impl.RequestUriPartial;
import static org.openrepose.rnxp.decoder.DecoderState.*;
import static org.openrepose.rnxp.decoder.AsciiCharacterConstant.*;
import static org.jboss.netty.buffer.ChannelBuffers.*;

public class HttpResponseDecoder extends FrameDecoder {

    private final char[] HTTP_VERSION_HEAD = new char[]{'H', 'T', 'T', 'P', '/'};
    private final boolean CASE_SENSITIVE = Boolean.TRUE, CASE_INSENSITIVE = Boolean.FALSE;
    private final HeaderProcessor fHeadHeaderProcessor;
    private HeaderProcessor currentHeaderProcessor;
    private ChannelBuffer headerKeyBuffer, characterBuffer;
    private DecoderState currentState;
    private int skipLength, stateCounter;
    private long contentLength, chunkLength;
    private boolean readChunkedContent;

    public HttpResponseDecoder() {
        currentState = READ_SC_PARSE_METHOD;
        contentLength = -1;
        readChunkedContent = false;
        characterBuffer = buffer(8192);

        currentHeaderProcessor = fHeadHeaderProcessor = new HeaderProcessor() {

            @Override
            public HttpErrorPartial processHeader(String key, String value) {
                HttpErrorPartial messagePartial = null;

                if (key.equals("content-length") && !readChunkedContent) {
                    try {
                        contentLength = Long.parseLong(value);
                    } catch (NumberFormatException nfe) {
                        messagePartial = HttpErrors.malformedContentLength();
                    }
                } else if (key.equals("transfer-encoding") && value.equalsIgnoreCase("chunked")) {
                    contentLength = -1;
                    readChunkedContent = true;
                }

                return messagePartial;
            }
        };
    }

    @Override
    protected Object decode(ChannelHandlerContext chc, Channel chnl, ChannelBuffer buffer) throws Exception {
        if (shouldSkipByte()) {
            skipBytes(buffer);
        } else {
            try {
                switch (state()) {
                    case READ_SC_PARSE_METHOD:
                        return readSingleCharacterParsableMethod(buffer);

                    case READ_MC_PARSE_METHOD:
                        return readMultiCharacterParsableMethod(buffer);

                    case READ_URI:
                        return readRequestURI(buffer);

                    case READ_VERSION:
                        return readVersion(buffer);

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

                return HttpErrors.bufferOverflow(state());
            }
        }

        return null;
    }

    private static String flushBuffer(ChannelBuffer buffer) {
        final StringWriter stringWriter = new StringWriter();

        while (buffer.readable()) {
            stringWriter.append((char) buffer.readByte());
        }

        return stringWriter.toString();
    }

    private HttpMessagePartial readSingleCharacterParsableMethod(ChannelBuffer buffer) {
        final char nextCharacter = (char) buffer.readByte();

        for (HttpMethod method : HttpMethod.SC_PARSE_METHODS) {
            if (nextCharacter == method.getMatcherFragment()[0]) {
                final RequestMethodPartial methodPartial = new RequestMethodPartial(HttpMessageComponent.REQUEST_METHOD, method);

                // Skip the remaining method length plus the following whitespace
                setSkip(method.getSkipLength() + 1);
                updateState(READ_URI);

                return methodPartial;
            }
        }

        updateState(READ_MC_PARSE_METHOD);

        return null;
    }

    private HttpMessagePartial readMultiCharacterParsableMethod(ChannelBuffer buffer) {
        final char nextCharacter = (char) buffer.readByte();
        HttpMessagePartial messagePartial = HttpErrors.methodNotImplemented();

        for (HttpMethod method : HttpMethod.MC_PARSE_METHODS) {
            if (nextCharacter == method.getMatcherFragment()[1]) {
                messagePartial = new RequestMethodPartial(HttpMessageComponent.REQUEST_METHOD, method);

                // Skip the remaining method length plus the following whitespace
                setSkip(method.getSkipLength() + 1);
                updateState(READ_URI);

                break;
            }
        }

        return messagePartial;
    }

    private ControlCharacter readUntil(ChannelBuffer buffer, ChannelBuffer charBuffer, boolean caseSensitive, AsciiCharacterConstant... controlCharacterSet) {
        final char nextCharacter = (char) buffer.readByte();

        for (AsciiCharacterConstant controlCharacter : controlCharacterSet) {
            if (controlCharacter.matches(nextCharacter)) {
                return new ControlCharacter(controlCharacter);
            }
        }

        charBuffer.ensureWritableBytes(1);
        charBuffer.writeByte(caseSensitive ? nextCharacter : Character.toLowerCase(nextCharacter));

        return null;
    }

    private HttpMessagePartial readRequestURI(ChannelBuffer socketBuffer) {
        final ControlCharacter readChar = readUntil(socketBuffer, characterBuffer, CASE_SENSITIVE, SPACE);

        if (readChar != null) {
            // TODO: Validate
            final RequestUriPartial uriPartial = new RequestUriPartial(HttpMessageComponent.REQUEST_URI, flushBuffer(characterBuffer));

            // Skip the next bit of whitespace
            setSkip(1);

            // Set counter to zero - it's used in parsing the HTTP Version
            stateCounter = 0;

            updateState(READ_VERSION);

            return uriPartial;
        }

        return null;
    }

    private HttpMessagePartial readVersion(ChannelBuffer socketBuffer) {
        HttpMessagePartial messagePartial = null;

        if (stateCounter < 5) {
            final char nextCharacter = (char) socketBuffer.readByte();

            if (Character.toUpperCase(nextCharacter) != HTTP_VERSION_HEAD[stateCounter++]) {
                messagePartial = HttpErrors.badVersion();
            }
        } else {
            if (readUntil(socketBuffer, characterBuffer, CASE_SENSITIVE, CARRIAGE_RETURN) != null) {
                messagePartial = new HttpVersionPartial(HttpMessageComponent.HTTP_VERSION, flushBuffer(characterBuffer));

                // Skip the next bit of whitespace
                setSkip(1);

                // Set ourselves up to process head headers
                currentHeaderProcessor = fHeadHeaderProcessor;
                updateState(READ_HEADER_KEY);
            }
        }

        return messagePartial;
    }

    private HttpMessagePartial readHeaderKey(ChannelBuffer buffer) {
        final ControlCharacter controlCharacter = readUntil(buffer, characterBuffer, CASE_INSENSITIVE, COLON, CARRIAGE_RETURN);
        HttpMessagePartial messagePartial = null;

        if (controlCharacter != null) {
            switch (controlCharacter.getCharacterConstant()) {
                case COLON:
                    // The header key must have data
                    if (characterBuffer.readable()) {
                        // Buffer pointer swap
                        final ChannelBuffer temp = headerKeyBuffer;
                        headerKeyBuffer = characterBuffer;
                        characterBuffer = temp != null ? temp : buffer(8192);

                        updateState(READ_HEADER_VALUE);
                    } else {
                        messagePartial = HttpErrors.badHeaderKey();
                    }

                    break;

                case CARRIAGE_RETURN:
                    // Skip the expected LF
                    setSkip(1);

                    if (contentLength > 0) {
                        messagePartial = new EmptyHttpMessagePartial(HttpMessageComponent.CONTENT_START);
                        updateState(READ_CONTENT);
                    } else if (readChunkedContent) {
                        messagePartial = new EmptyHttpMessagePartial(HttpMessageComponent.CONTENT_START);
                        updateState(READ_CHUNK_LENGTH);
                    } else {
                        messagePartial = new EmptyHttpMessagePartial(HttpMessageComponent.MESSAGE_END_NO_CONTENT);
                        updateState(READ_END);
                    }
            }
        }

        return messagePartial;
    }

    private HttpMessagePartial readHeaderValue(ChannelBuffer buffer, HeaderProcessor headerProcessor) {
        HttpMessagePartial messagePartial = null;

        if (readUntil(buffer, characterBuffer, CASE_SENSITIVE, CARRIAGE_RETURN) != null) {
            // Skip the expected LF
            setSkip(1);

            // Dump the buffers to encoded strings
            final String headerKey = flushBuffer(headerKeyBuffer);
            final String headerValue = flushBuffer(characterBuffer);

            if (headerProcessor != null) {
                messagePartial = headerProcessor.processHeader(headerKey, headerValue);
            }

            updateState(READ_HEADER_KEY);

            if (messagePartial == null) {
                messagePartial = new HeaderPartial(HttpMessageComponent.ENTITY_HEADER, headerKey, headerValue);
            }
        }

        return messagePartial;
    }

    private HttpMessagePartial readContent(ChannelBuffer buffer) {
        HttpMessagePartial messagePartial = null;

        final byte readByte = buffer.readByte();
        contentLength--;

        if (contentLength > 0) {
            messagePartial = new ContentMessagePartial(readByte);
        } else {
            messagePartial = new ContentMessagePartial(HttpMessageComponent.MESSAGE_END_WITH_CONTENT, readByte);
            updateState(READ_END);
        }

        return messagePartial;
    }

    private HttpMessagePartial readContentChunkLength(ChannelBuffer buffer) {
        HttpMessagePartial messagePartial = null;

        if (readUntil(buffer, characterBuffer, CASE_INSENSITIVE, CARRIAGE_RETURN) != null) {
            // Skip the expected LF
            setSkip(1);

            parseChunkLength(flushBuffer(characterBuffer));
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
            readChunkedContent = false;

            // We're done reading content
            updateState(READ_HEADER_KEY);
        } else {
            chunkLength = newChunkLength;
            updateState(READ_CONTENT_CHUNKED);
        }
    }

    private HttpMessagePartial readContentChunked(ChannelBuffer buffer) {
        HttpMessagePartial messagePartial = null;

        final byte nextByte = buffer.readByte();
        chunkLength--;

        if (CARRIAGE_RETURN.matches((char) nextByte)) {
            // Skip expected LF
            setSkip(1);

            updateState(READ_CHUNK_LENGTH);
        } else {
            messagePartial = new ContentMessagePartial(nextByte);
        }

        return messagePartial;
    }

    private void setSkip(int newSkipCount) {
        skipLength = newSkipCount;
    }

    private void skipBytes(ChannelBuffer b) {
        b.skipBytes(1);
        skipLength--;
    }

    private boolean shouldSkipByte() {
        return skipLength > 0;
    }

    private DecoderState state() {
        return currentState;
    }

    public void updateState(DecoderState state) {
        currentState = state;
    }
}