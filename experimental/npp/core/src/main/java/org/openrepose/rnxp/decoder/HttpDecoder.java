package org.openrepose.rnxp.decoder;

import org.openrepose.rnxp.http.domain.HttpMessageComponent;
import org.openrepose.rnxp.http.domain.HttpMethod;
import org.openrepose.rnxp.http.domain.HttpPartial;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import static org.openrepose.rnxp.decoder.DecoderState.*;
import static org.openrepose.rnxp.decoder.AsciiCharacterConstant.*;
//import static org.jboss.netty.buffer.ChannelBuffers.*;

public class HttpDecoder extends FrameDecoder {

    private StringBuilder stringBuilder;
    private DecoderState currentState;
    private int skipCount, stateCounter, contentLength;

    public HttpDecoder() {
        currentState = READ_SC_PARSE_METHOD;
        contentLength = 0;
        stringBuilder = new StringBuilder();
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
                    throw new Exception("Not supported!");

                case READ_URI:
                    return readRequestURI(buffer);

                case READ_VERSION:
                    return readVersion(buffer);

                case READ_HEADER:
                    return readHeader(buffer);

                case READ_CONTENT:
                    setSkip(contentLength);
                    updateState(CONTENT_END);
                    break;

                case CONTENT_END:
                    return new HttpPartial(HttpMessageComponent.CONTENT_END);
            }
        }

        return null;
    }

    private String flushInternalBufferToString() {
        final String flushedString = stringBuilder.toString();
        stringBuilder = new StringBuilder();

        return flushedString;
    }

    private HttpPartial readSingleCharacterParsableMethod(ChannelBuffer buffer) {
        final char nextCharacter = (char) buffer.readByte();

        for (HttpMethod method : HttpMethod.SC_PARSE_METHODS) {
            if (nextCharacter == method.getMatcherFragment()[0]) {
                final HttpPartial methodPartial = new HttpPartial(HttpMessageComponent.REQUEST_METHOD);
                methodPartial.setMethod(method);

                // Skip the remaining method length plus the following whitespace
                setSkip(method.getMethodLength() + 1);
                updateState(READ_URI);

                return methodPartial;
            }
        }

        updateState(READ_MC_PARSE_METHOD);

        return null;
    }

    private boolean readUntilControlCharacter(ChannelBuffer buffer, AsciiCharacterConstant controlCharacter, StringBuilder stringBuffer) {
        while (buffer.readableBytes() > 0) {
            final char nextCharacter = (char) buffer.readByte();

            if (controlCharacter.matches(nextCharacter)) {
                return true;
            }

            stringBuffer.append(nextCharacter);
        }

        return false;
    }

    private HttpPartial readRequestURI(ChannelBuffer buffer) {
        if (readUntilControlCharacter(buffer, SPACE, stringBuilder)) {
            final HttpPartial uriPartial = new HttpPartial(HttpMessageComponent.REQUEST_URI);
            uriPartial.setPartial(flushInternalBufferToString());

            // Skip the next bit of whitespace
            setSkip(1);
            updateState(READ_VERSION);

            return uriPartial;
        }

        return null;
    }

    private HttpPartial readVersion(ChannelBuffer buffer) {
        if (stateCounter < 4) {
            final char nextCharacter = (char) buffer.readByte();

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
        } else if (readUntilControlCharacter(buffer, CARRIAGE_RETURN, stringBuilder)) {
            final HttpPartial httpVersionPartial = new HttpPartial(HttpMessageComponent.HTTP_VERSION);
            httpVersionPartial.setPartial(flushInternalBufferToString());

            // Skip the next bit of whitespace
            setSkip(1);
            updateState(READ_HEADER);

            return httpVersionPartial;
        }

        return null;
    }

    private HttpPartial readHeader(ChannelBuffer buffer) {
        if (readUntilControlCharacter(buffer, CARRIAGE_RETURN, stringBuilder)) {
            if (stringBuilder.length() > 0) {
                // The string buffer has data - this is probably a header                
                final HttpPartial headerPartial = new HttpPartial(HttpMessageComponent.HEADER);
                headerPartial.setPartial(flushInternalBufferToString());

                // Skip the expected LF
                setSkip(1);

                return headerPartial;
            } else {
                // Encountered a CR without actually reading anything - This signifies the head of the headers
                HttpPartial messagePartial;

                // Skip the expected LF and the content
                setSkip(1);

                if (contentLength > 0) {
                    messagePartial = new HttpPartial(HttpMessageComponent.CONTENT_START);
                    updateState(READ_CONTENT);
                } else {
                    messagePartial = new HttpPartial(HttpMessageComponent.CONTENT_END);
                    updateState(CONTENT_END);
                }

                return messagePartial;
            }
        }

        return null;
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