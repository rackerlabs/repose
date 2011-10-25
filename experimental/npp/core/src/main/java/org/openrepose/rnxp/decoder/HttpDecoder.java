package org.openrepose.rnxp.decoder;

import java.io.StringWriter;
import org.openrepose.rnxp.http.domain.HttpMessageComponent;
import org.openrepose.rnxp.http.domain.HttpMethod;
import org.openrepose.rnxp.http.domain.HttpPartial;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import static org.openrepose.rnxp.decoder.DecoderState.*;
import static org.openrepose.rnxp.decoder.AsciiCharacterConstant.*;
import static org.jboss.netty.buffer.ChannelBuffers.*;

public class HttpDecoder extends FrameDecoder {

    private ChannelBuffer headerKey, charBuffer;
    private DecoderState currentState;
    private int skipCount, stateCounter, contentLength;

    public HttpDecoder() {
        currentState = READ_SC_PARSE_METHOD;
        contentLength = 0;
        charBuffer = buffer(8192);
    }

    @Override
    protected Object decode(ChannelHandlerContext chc, Channel chnl, ChannelBuffer buffer) throws Exception {
        if (shouldSkipByte()) {
            skipBytes(buffer);
        } else {
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

                case STREAM_REMAINING:
                    return buffer.readByte();

                case READ_END:
                // ?
            }
        }

        return null;
    }

    private String flushBufferToString(ChannelBuffer buffer) {
        final StringWriter stringWriter = new StringWriter();

        while (buffer.readable()) {
            stringWriter.append((char) buffer.readByte());
        }

        return stringWriter.toString();
    }

    private HttpPartial readSingleCharacterParsableMethod(ChannelBuffer buffer) {
        final char nextCharacter = (char) buffer.readByte();

        for (HttpMethod method : HttpMethod.SC_PARSE_METHODS) {
            if (nextCharacter == method.getMatcherFragment()[0]) {
                final HttpPartial methodPartial = new HttpPartial(HttpMessageComponent.REQUEST_METHOD);
                methodPartial.setMethod(method);

                // Skip the remaining method length plus the following whitespace
                setSkip(method.getSkipLength() + 1);
                updateState(READ_URI);

                return methodPartial;
            }
        }

        updateState(READ_MC_PARSE_METHOD);

        return null;
    }

    private HttpPartial readMultiCharacterParsableMethod(ChannelBuffer buffer) {
        final char nextCharacter = (char) buffer.readByte();

        for (HttpMethod method : HttpMethod.MC_PARSE_METHODS) {
            if (nextCharacter == method.getMatcherFragment()[1]) {
                final HttpPartial methodPartial = new HttpPartial(HttpMessageComponent.REQUEST_METHOD);
                methodPartial.setMethod(method);

                // Skip the remaining method length plus the following whitespace
                setSkip(method.getSkipLength() + 1);
                updateState(READ_URI);

                return methodPartial;
            }
        }

        // TODO: Failcase
        return null;
    }

    private final boolean CASE_SENSITIVE = Boolean.TRUE, CASE_INSENSITIVE = Boolean.FALSE;
    
    private ControlCharacter readUntil(ChannelBuffer buffer, ChannelBuffer charBuffer, boolean caseSensitive, AsciiCharacterConstant... controlCharacterSet) {
        while (buffer.readableBytes() > 0) {
            final char nextCharacter = caseSensitive ? Character.toLowerCase((char) buffer.readByte()) : (char) buffer.readByte();

            for (AsciiCharacterConstant controlCharacter : controlCharacterSet) {
                if (controlCharacter.matches(nextCharacter)) {
                    return new ControlCharacter(controlCharacter);
                }
            }

            try {
                charBuffer.ensureWritableBytes(1);
                charBuffer.writeByte(nextCharacter);
            } catch (IndexOutOfBoundsException boundsException) {
                // TODO: Failcase
            }
        }

        return null;
    }

    private HttpPartial readRequestURI(ChannelBuffer socketBuffer) {
        final ControlCharacter readChar = readUntil(socketBuffer, charBuffer, CASE_SENSITIVE, SPACE);

        if (readChar != null) {
            final HttpPartial uriPartial = new HttpPartial(HttpMessageComponent.REQUEST_URI);
            uriPartial.setPartial(flushBufferToString(charBuffer));

            // Skip the next bit of whitespace
            setSkip(1);
            updateState(READ_VERSION);

            return uriPartial;
        }

        return null;
    }

    private HttpPartial readVersion(ChannelBuffer socketBuffer) {
        if (stateCounter < 5) {
            final char nextCharacter = (char) socketBuffer.readByte();

            switch (stateCounter++) {
                case 0:
                    if (nextCharacter != 'H') {
                        // TODO:Implement Validate that this is an error
                    }
                    break;

                case 1:
                    if (nextCharacter != 'T') {
                        // TODO:Implement Validate that this is an error
                    }
                    break;

                case 2:
                    if (nextCharacter != 'T') {
                        // TODO:Implement Validate that this is an error
                    }
                    break;

                case 3:
                    if (nextCharacter != 'P') {
                        // TODO:Implement Validate that this is an error
                    }
                    break;

                case 4:
                    if (nextCharacter != '/') {
                        // TODO:Implement Validate that this is an error
                    }
                    break;
            }
        } else {
            if (readUntil(socketBuffer, charBuffer, CASE_SENSITIVE, CARRIAGE_RETURN) != null) {
                final HttpPartial httpVersionPartial = new HttpPartial(HttpMessageComponent.HTTP_VERSION);
                httpVersionPartial.setPartial(flushBufferToString(charBuffer));

                // Skip the next bit of whitespace
                setSkip(1);
                updateState(READ_HEADER_KEY);

                return httpVersionPartial;
            }
        }

        return null;
    }

    private HttpPartial readHeaderKey(ChannelBuffer buffer) {
        final ControlCharacter controlCharacter = readUntil(buffer, charBuffer, CASE_INSENSITIVE, COLON, CARRIAGE_RETURN);
        HttpPartial messagePartial = null;
        
        if (controlCharacter != null) {
            switch (controlCharacter.getCharacterConstant()) {
                case COLON:
                    // The string buffer should have data
                    if (!charBuffer.readable()) {
                        // TODO: error case 400
                    }

                    // Pointer swap
                    final ChannelBuffer temp = headerKey;
                    headerKey = charBuffer;
                    charBuffer = temp;
                    
                    break;

                case CARRIAGE_RETURN:
                    // Skip the expected LF
                    setSkip(1);

                    if (headerKey.readable()) {
                        // The header key buffer has data - this is probably a header                
                        messagePartial = new HttpPartial(HttpMessageComponent.HEADER);
                        messagePartial.setHeaderKey(flushBufferToString(headerKey));
                        messagePartial.setHeaderValue(flushBufferToString(charBuffer));
                    } else {
                        if (contentLength > 0) {
                            messagePartial = new HttpPartial(HttpMessageComponent.CONTENT_START);
                            updateState(STREAM_REMAINING);
                        } else {
                            messagePartial = new HttpPartial(HttpMessageComponent.MESSAGE_END);
                            updateState(READ_END);
                        }
                    }
            }
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

    private void updateState(DecoderState state) {
        switch (state) {
            case READ_VERSION:
                stateCounter = 0;
                break;
        }

        currentState = state;
    }
}