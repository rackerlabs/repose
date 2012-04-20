package org.openrepose.rnxp.decoder;

import java.io.StringWriter;
import org.jboss.netty.buffer.ChannelBuffer;
import static org.jboss.netty.buffer.ChannelBuffers.buffer;
import static org.openrepose.rnxp.decoder.AsciiCharacterConstant.CARRIAGE_RETURN;
import static org.openrepose.rnxp.decoder.AsciiCharacterConstant.COLON;
import static org.openrepose.rnxp.decoder.DecoderState.*;
import org.openrepose.rnxp.decoder.partial.ContentMessagePartial;
import org.openrepose.rnxp.decoder.partial.EmptyHttpMessagePartial;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.decoder.partial.impl.HeaderPartial;
import org.openrepose.rnxp.decoder.partial.impl.HttpVersionPartial;
import org.openrepose.rnxp.decoder.processor.HeaderProcessor;
import org.openrepose.rnxp.http.HttpMessageComponent;

/**
 *
 * @author zinic
 */
public abstract class AbstractHttpMessageDecoder implements HttpMessageDecoder {

    private static final char[] HTTP_VERSION_HEAD = new char[]{'H', 'T', 'T', 'P', '/'};
    private static final boolean CASE_SENSITIVE = Boolean.TRUE, CASE_INSENSITIVE = Boolean.FALSE;
    private static final short DEFAULT_BUFFER_SIZE = 8192;
    private ChannelBuffer characterBufferAside, currentBuffer;
    private ContentPresence contentPresence;
    private int decoderSkipLength, stateCounter;
    private long contentLength;
    private DecoderState state;

    public AbstractHttpMessageDecoder() {
        init();
    }

    private void init() {
        state = initialState();

        // Set counter to zero - it's used in parsing the HTTP Version
        stateCounter = 0;

        contentLength = -1;
        contentPresence = ContentPresence.NO_CONTENT;

        currentBuffer = buffer(DEFAULT_BUFFER_SIZE);
    }

    private void skip(ChannelBuffer b) {
        b.skipBytes(1);
        decoderSkipLength--;
    }

    private boolean shouldSkip() {
        return decoderSkipLength > 0;
    }

    @Override
    public HttpMessagePartial decode(ChannelBuffer cb) {
        if (shouldSkip()) {
            skip(cb);
            
            return null;
        }

        return httpDecode(cb);
    }

    protected abstract HttpMessagePartial httpDecode(ChannelBuffer cb);

    protected abstract DecoderState initialState();

    public boolean characterBufferReadable() {
        return currentBuffer.readable();
    }

    /**
     * Backing buffer swap
     */
    public void flipBuffers() {
        final ChannelBuffer temp = characterBufferAside;
        characterBufferAside = currentBuffer;
        currentBuffer = temp != null ? temp : buffer(8192);
    }

    public String flushCharacterBuffer() {
        return flushToString(currentBuffer);
    }

    public static String flushToString(ChannelBuffer buffer) {
        final StringWriter stringWriter = new StringWriter();

        while (buffer.readable()) {
            stringWriter.append((char) buffer.readByte());
        }

        return stringWriter.toString();
    }

    public ControlCharacter readUntilCaseInsensitive(ChannelBuffer source, AsciiCharacterConstant... controlCharacterSet) {
        return readUntil(source, currentBuffer, CASE_INSENSITIVE, controlCharacterSet);
    }

    public ControlCharacter readUntilCaseSensitive(ChannelBuffer source, AsciiCharacterConstant... controlCharacterSet) {
        return readUntil(source, currentBuffer, CASE_SENSITIVE, controlCharacterSet);
    }

    private static ControlCharacter readUntil(ChannelBuffer buffer, ChannelBuffer charBuffer, boolean caseSensitive, AsciiCharacterConstant... controlCharacterSet) {
        final char nextCharacter = (char) buffer.readByte();

        for (AsciiCharacterConstant controlCharacter : controlCharacterSet) {
            if (controlCharacter.matches(nextCharacter)) {
                return new ControlCharacter(controlCharacter);
            }
        }

        // TODO: Consider more rich return type instead of relying on exception generation here
        charBuffer.ensureWritableBytes(1);
        charBuffer.writeByte(caseSensitive ? nextCharacter : Character.toLowerCase(nextCharacter));

        return null;
    }

    public HttpMessagePartial readHttpVersion(ChannelBuffer socketBuffer, AsciiCharacterConstant expectedControlCharacter) {
        HttpMessagePartial messagePartial = null;

        if (stateCounter < 5) {
            final char nextCharacter = (char) socketBuffer.readByte();

            if (Character.toUpperCase(nextCharacter) != HTTP_VERSION_HEAD[stateCounter++]) {
                messagePartial = HttpErrors.badVersion();
            }
        } else {
            if (readUntilCaseSensitive(socketBuffer, expectedControlCharacter) != null) {
                messagePartial = new HttpVersionPartial(HttpMessageComponent.HTTP_VERSION, flushToString(currentBuffer));
                stateCounter = 0;
            }
        }

        return messagePartial;
    }

    public HttpMessagePartial readHeaderKey(ChannelBuffer buffer, HeaderProcessor headerProcessor) {
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
                    if (headerProcessor != null) {
                        headerProcessor.finishedReadingHeaders();
                    }

                    switch (getContentPresence()) {
                        case CHUNKED:
                            messagePartial = new EmptyHttpMessagePartial(HttpMessageComponent.CONTENT_START);
                            setDecoderState(READ_CHUNK_LENGTH);

                            // Skip the expected LF
                            skipFollowingBytes(1);
                            break;

                        case STATIC_LENGTH:
                            messagePartial = new EmptyHttpMessagePartial(HttpMessageComponent.CONTENT_START);
                            setDecoderState(READ_CONTENT);

                            // Skip the expected LF
                            skipFollowingBytes(1);
                            break;

                        default:
                            messagePartial = new EmptyHttpMessagePartial(HttpMessageComponent.MESSAGE_END_NO_CONTENT);
                            setDecoderState(STOP);
                    }
            }
        }

        return messagePartial;

    }

    public HttpMessagePartial readHeaderValue(ChannelBuffer buffer, HeaderProcessor headerProcessor) {
        HttpMessagePartial messagePartial = null;

        if (readUntilCaseSensitive(buffer, CARRIAGE_RETURN) != null) {
            // Skip the expected LF
            skipFollowingBytes(1);

            // Dump the buffers to encoded strings
            final String headerValue = flushCharacterBuffer().trim();
            flipBuffers();

            final String headerKey = flushCharacterBuffer().trim();

            if (headerProcessor != null) {
                messagePartial = headerProcessor.processHeader(headerKey, headerValue);
            }

            setDecoderState(READ_HEADER_KEY);

            if (messagePartial == null) {
                messagePartial = new HeaderPartial(HttpMessageComponent.HEADER, headerKey, headerValue);
            }
        }

        return messagePartial;
    }

    public HttpMessagePartial readContent(ChannelBuffer buffer) {
        HttpMessagePartial messagePartial = null;

        final byte readByte = buffer.readByte();
        contentRead();

        if (getContentLength() > 0) {
            messagePartial = new ContentMessagePartial(readByte);
        } else {
            messagePartial = new ContentMessagePartial(HttpMessageComponent.MESSAGE_END_WITH_CONTENT, readByte);
            setDecoderState(STOP);
        }

        return messagePartial;
    }

    public HttpMessagePartial readContentChunkLength(ChannelBuffer buffer) {
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
           throw new IllegalArgumentException("Chunk length must be > 0");
        } else if (newChunkLength == 0) {
            setContentPresence(ContentPresence.NO_CONTENT);

            // We're done reading content
            setDecoderState(READ_HEADER_KEY);
        } else {
            setContentLength(newChunkLength);
            setDecoderState(READ_CONTENT_CHUNKED);
        }
    }

    public HttpMessagePartial readContentChunked(ChannelBuffer buffer) {
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

    public ContentPresence getContentPresence() {
        return contentPresence;
    }

    public void setContentPresence(ContentPresence contentPresence) {
        this.contentPresence = contentPresence;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public void contentRead() {
        contentLength--;
    }

    public void contentRead(long contentLenght) {
        this.contentLength -= contentLenght;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setDecoderState(DecoderState state) {
        this.state = state;
    }

    public DecoderState getDecoderState() {
        return state;
    }

    public void skipFollowingBytes(int decoderSkipLength) {
        this.decoderSkipLength = decoderSkipLength;
    }
}
