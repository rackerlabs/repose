package com.rackspace.papi.commons.util.io.buffer;

import java.io.IOException;

public class CyclicByteBuffer implements ByteBuffer {

    private final static ByteArrayProvider DEFAULT_BYTE_ARRAY_PROVIDER = HeapspaceByteArrayProvider.getInstance();
    private final static int DEFAULT_BUFFER_SIZE = 2048; //in bytes
    private final ByteArrayProvider byteArrayProvider;
    private int nextWritableIndex, nextReadableIndex;
    private boolean hasElements;
    private byte[] buffer;

    public CyclicByteBuffer() {
        this(DEFAULT_BYTE_ARRAY_PROVIDER, 0, 0, false);
    }
    
    public CyclicByteBuffer(ByteArrayProvider byteArrayProvider) {
        this(byteArrayProvider, 0, 0, false);
    }
    
    protected CyclicByteBuffer(ByteArrayProvider byteArrayProvider, int nextWritableIndex, int nextReadableIndex, boolean hasElements) {
        this.byteArrayProvider = byteArrayProvider;
        this.nextWritableIndex = nextWritableIndex;
        this.nextReadableIndex = nextReadableIndex;
        this.hasElements = hasElements;

        buffer = byteArrayProvider.allocate(DEFAULT_BUFFER_SIZE);
    }

    public CyclicByteBuffer(ByteArrayProvider byteArrayProvider, CyclicByteBuffer byteBuffer) {
        this.byteArrayProvider = byteArrayProvider;

        final int readableLength = byteBuffer.available();
        final int allocationSize = (readableLength > DEFAULT_BUFFER_SIZE? readableLength: DEFAULT_BUFFER_SIZE);
        buffer = byteArrayProvider.allocate(allocationSize);

        if (byteBuffer.nextReadableIndex + readableLength > byteBuffer.buffer.length) {
            final int trimmedLength = byteBuffer.buffer.length - byteBuffer.nextReadableIndex;

            System.arraycopy(byteBuffer.buffer, byteBuffer.nextReadableIndex, buffer, 0, trimmedLength);
            System.arraycopy(byteBuffer.buffer, 0, buffer, trimmedLength, readableLength - trimmedLength);
        } else {
            System.arraycopy(byteBuffer.buffer, byteBuffer.nextReadableIndex, buffer, 0, readableLength);
        }

        this.nextReadableIndex = 0;
        this.nextWritableIndex = 0;
        this.hasElements = byteBuffer.available() > 0;
    }

    @Override
    public void clear() {
        nextReadableIndex = 0;
        nextWritableIndex = 0;
        hasElements = false;
    }

    @Override
    public int skip(int len) {
        int bytesSkipped = len;

        if (len > available()) {
            bytesSkipped = available();

            nextReadableIndex = 0;
            nextWritableIndex = 0;
        } else {
            nextReadableIndex = nextReadableIndex + len < buffer.length
                    ? nextReadableIndex + len
                    : len - (buffer.length - nextReadableIndex);
        }

        if (nextReadableIndex == nextWritableIndex) {
            hasElements = false;
        }

        return bytesSkipped;
    }

    @Override
    public int available() {
        if (nextWritableIndex == nextReadableIndex && hasElements) {
            return buffer.length;
        }

        return nextWritableIndex < nextReadableIndex ? nextWritableIndex + (buffer.length - nextReadableIndex) : nextWritableIndex - nextReadableIndex;
    }

    @Override
    public int remaining() {
        if (nextWritableIndex == nextReadableIndex && hasElements) {
            return 0;
        }

        return nextWritableIndex < nextReadableIndex ? nextReadableIndex - nextWritableIndex : buffer.length - nextWritableIndex + nextReadableIndex;
    }

    private void grow(int minLength) {
        final int newSize = buffer.length + buffer.length * (minLength / buffer.length + 1);
        final byte[] newBuffer = byteArrayProvider.allocate(newSize);

        final int read = get(newBuffer, 0, newSize);
        buffer = newBuffer;

        nextWritableIndex = read;
        nextReadableIndex = 0;
        hasElements = true;
    }

    @Override
    public int put(byte[] b, int off, int len) {
        final int remaining = remaining();

        if (remaining < len) {
            grow(len - remaining);
        }

        if (nextWritableIndex + len > buffer.length) {
            final int trimmedLength = buffer.length - nextWritableIndex;

            System.arraycopy(b, off, buffer, nextWritableIndex, trimmedLength);
            System.arraycopy(b, off + trimmedLength, buffer, 0, len - trimmedLength);
            nextWritableIndex = len - trimmedLength;
        } else {
            System.arraycopy(b, off, buffer, nextWritableIndex, len);
            nextWritableIndex = nextWritableIndex + len < buffer.length ? nextWritableIndex + len : len - (buffer.length - nextWritableIndex);
        }

        hasElements = true;

        return len;
    }

    @Override
    public int get(byte[] b, int off, int len) {
        final int readableLength = available() > len ? len : available();

        if (hasElements) {
            if (nextReadableIndex + readableLength > buffer.length) {
                final int trimmedLength = buffer.length - nextReadableIndex;

                System.arraycopy(buffer, nextReadableIndex, b, off, trimmedLength);
                System.arraycopy(buffer, 0, b, off + trimmedLength, readableLength - trimmedLength);
                nextReadableIndex = readableLength - trimmedLength;
            } else {
                System.arraycopy(buffer, nextReadableIndex, b, off, readableLength);
                nextReadableIndex = nextReadableIndex + readableLength < buffer.length ? nextReadableIndex + readableLength : readableLength - (buffer.length - nextReadableIndex);
            }

            if (nextWritableIndex == nextReadableIndex) {
                hasElements = false;
            }
        }

        return readableLength;
    }

    @Override
    public void put(byte b) {
        put(new byte[]{b}, 0, 1);
    }

    @Override
    public byte get() {
        final byte[] singleByte = new byte[]{-1};

        if (available() > 0) {
            get(singleByte, 0, 1);
        }

        return singleByte[0];
    }

    @Override
    public int get(byte[] b) throws IOException {
        return get(b, 0, b.length);
    }

    @Override
    public int put(byte[] b) throws IOException {
        return put(b, 0, b.length);
    }

    @Override
    public ByteBuffer copy() {
        return new CyclicByteBuffer(byteArrayProvider, this);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return copy();
    }
}
