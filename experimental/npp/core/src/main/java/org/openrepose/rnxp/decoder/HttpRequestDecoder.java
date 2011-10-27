package org.openrepose.rnxp.decoder;

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
import org.openrepose.rnxp.decoder.partial.impl.HttpVersionPartial;
import org.openrepose.rnxp.decoder.partial.impl.RequestMethodPartial;
import org.openrepose.rnxp.decoder.partial.impl.RequestUriPartial;
import static org.openrepose.rnxp.decoder.DecoderState.*;
import static org.openrepose.rnxp.decoder.AsciiCharacterConstant.*;
import static org.jboss.netty.buffer.ChannelBuffers.*;

public class HttpRequestDecoder extends FrameDecoder {

    private final char[] HTTP_VERSION_HEAD = new char[]{'H', 'T', 'T', 'P', '/'};
    private final boolean CASE_SENSITIVE = Boolean.TRUE, CASE_INSENSITIVE = Boolean.FALSE;
    private ChannelBuffer headerKey, charBuffer;
    private DecoderState currentState;
    private int skipCount, stateCounter;
    private long contentLength;
    private boolean chunked;

    public HttpRequestDecoder() {
        currentState = READ_SC_PARSE_METHOD;
        contentLength = -1;
        chunked = false;
        charBuffer = buffer(8192);
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
                        return readHeaderValue(buffer);

                    case READ_CONTENT:
                        return readContent(buffer);

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

    private void processHeader(final String headerKeyString, final String headerKeyValue) throws NumberFormatException {
        if (contentLength == -1 && headerKeyString.equals("content-length")) {
            contentLength = Long.parseLong(headerKeyValue);
        } else if (!chunked && headerKeyString.equals("transfer-encoding") && headerKeyValue.equalsIgnoreCase("chunked")) {
            chunked = true;
        }
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
        final ControlCharacter readChar = readUntil(socketBuffer, charBuffer, CASE_SENSITIVE, SPACE);

        if (readChar != null) {
            // TODO: Validate
            final RequestUriPartial uriPartial = new RequestUriPartial(HttpMessageComponent.REQUEST_URI, flushBuffer(charBuffer));

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
            if (readUntil(socketBuffer, charBuffer, CASE_SENSITIVE, CARRIAGE_RETURN) != null) {
                messagePartial = new HttpVersionPartial(HttpMessageComponent.HTTP_VERSION, flushBuffer(charBuffer));

                // Skip the next bit of whitespace
                setSkip(1);
                updateState(READ_HEADER_KEY);
            }
        }

        return messagePartial;
    }

    private HttpMessagePartial readHeaderKey(ChannelBuffer buffer) {
        final ControlCharacter controlCharacter = readUntil(buffer, charBuffer, CASE_INSENSITIVE, COLON, CARRIAGE_RETURN);
        HttpMessagePartial messagePartial = null;

        if (controlCharacter != null) {
            switch (controlCharacter.getCharacterConstant()) {
                case COLON:
                    // The header key must have data
                    if (charBuffer.readable()) {
                        // Buffer pointer swap
                        final ChannelBuffer temp = headerKey;
                        headerKey = charBuffer;
                        charBuffer = temp != null ? temp : buffer(8192);

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
                    } else {
                        messagePartial = new EmptyHttpMessagePartial(HttpMessageComponent.MESSAGE_END);
                        updateState(READ_END);
                    }
            }
        }

        return messagePartial;
    }

    private HttpMessagePartial readHeaderValue(ChannelBuffer buffer) {
        HttpMessagePartial messagePartial = null;

        if (readUntil(buffer, charBuffer, CASE_SENSITIVE, CARRIAGE_RETURN) != null) {
            // Skip the expected LF
            setSkip(1);

            try {
                // Dump the buffers to encoded strings
                final String headerKeyString = flushBuffer(headerKey);
                final String headerKeyValue = flushBuffer(charBuffer);

                processHeader(headerKeyString, headerKeyValue);

                messagePartial = new HeaderPartial(HttpMessageComponent.HEADER, headerKeyString, headerKeyValue);
                updateState(READ_HEADER_KEY);
            } catch (NumberFormatException nfe) {
                messagePartial = HttpErrors.malformedContentLength();
            }
        }

        return messagePartial;
    }

    private HttpMessagePartial readContent(ChannelBuffer buffer) {
        HttpMessagePartial messagePartial = null;

        if (contentLength > 0) {
            contentLength--;
            messagePartial = new ContentMessagePartial(HttpMessageComponent.HEADER, buffer.readByte());
        } else {
            updateState(READ_END);
            messagePartial = new EmptyHttpMessagePartial(HttpMessageComponent.MESSAGE_END);
        }

        return messagePartial;
    }

    private void setSkip(int newSkipCount) {
        skipCount = newSkipCount;
    }

    private void skipBytes(ChannelBuffer b) {
        final int readableBytes = b.readableBytes();
        final int byesAvailableToSkip = readableBytes > skipCount ? skipCount : readableBytes;

        b.skipBytes(byesAvailableToSkip);
        skipCount -= readableBytes;
    }

    private boolean shouldSkipByte() {
        return skipCount > 0;
    }

    private DecoderState state() {
        return currentState;
    }

    public void updateState(DecoderState state) {
        currentState = state;
    }
}