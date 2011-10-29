package org.openrepose.rnxp.decoder;

import java.io.StringWriter;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.decoder.partial.impl.HttpVersionPartial;
import org.openrepose.rnxp.http.HttpMessageComponent;

import static org.openrepose.rnxp.decoder.AsciiCharacterConstant.*;
import static org.jboss.netty.buffer.ChannelBuffers.*;

/**
 *
 * @author zinic
 */
public abstract class AbstractHttpMessageDecoder extends FrameDecoder {

    private static final char[] HTTP_VERSION_HEAD = new char[]{'H', 'T', 'T', 'P', '/'};
    private static final boolean CASE_SENSITIVE = Boolean.TRUE, CASE_INSENSITIVE = Boolean.FALSE;
    public static final short DEFAULT_BUFFER_SIZE = 8192;
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
    protected final Object decode(ChannelHandlerContext chc, Channel chnl, ChannelBuffer cb) throws Exception {
        if (shouldSkip()) {
            skip(cb);
            return null;
        }

        return httpDecode(chc, chnl, cb);
    }

    protected abstract Object httpDecode(ChannelHandlerContext chc, Channel chnl, ChannelBuffer cb) throws Exception;

    protected abstract DecoderState initialState();

    public boolean characterBufferReadable() {
        return currentBuffer.readable();
    }
    
    public void flipBuffers() {
        // Buffer pointer swap
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

                // Skip the next bit of whitespace
                skipFollowingBytes(1);
            }
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
