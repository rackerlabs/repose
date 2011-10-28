package org.openrepose.rnxp.decoder;

import java.io.StringWriter;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

/**
 *
 * @author zinic
 */
public abstract class AbstractHttpMessageDecoder extends FrameDecoder {

    private ContentPresence contentPresence;
    private int decoderSkipLength;
    private long contentLength;
    private DecoderState state;

    public AbstractHttpMessageDecoder() {
        init();
    }

    private void init() {
        state = initialState();

        contentLength = -1;
        contentPresence = ContentPresence.NO_CONTENT;
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

    public static String flushToString(ChannelBuffer buffer) {
        final StringWriter stringWriter = new StringWriter();

        while (buffer.readable()) {
            stringWriter.append((char) buffer.readByte());
        }

        return stringWriter.toString();
    }

    public static ControlCharacter readUntil(ChannelBuffer buffer, ChannelBuffer charBuffer, boolean caseSensitive, AsciiCharacterConstant... controlCharacterSet) {
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
