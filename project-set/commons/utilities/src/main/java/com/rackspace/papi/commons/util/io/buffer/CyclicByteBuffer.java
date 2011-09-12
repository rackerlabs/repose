package com.rackspace.papi.commons.util.io.buffer;

import com.rackspace.papi.commons.util.ArrayUtilities;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CyclicByteBuffer implements ByteBuffer {

    private final static int DEFAULT_BUFFER_SIZE = 2048; //in bytes
    
    private final Lock bufferLock;
    private final byte[] singleByte;

    private int nextWritableIndex, nextReadableIndex;
    private boolean hasElements;
    private byte[] buffer;

    public CyclicByteBuffer() {
        this(DEFAULT_BUFFER_SIZE);
    }

    public CyclicByteBuffer(int bufferSize) {
        this(new byte[bufferSize], false);
    }

    private CyclicByteBuffer(byte[] buffer, boolean hasElements) {
        this.nextWritableIndex = 0;
        this.nextReadableIndex = 0;
        this.hasElements = hasElements;
        this.buffer = ArrayUtilities.nullSafeCopy(buffer);

        singleByte = new byte[1];
        bufferLock = new ReentrantLock();
    }

    @Override
    public int skip(int len) {
        int bytesSkipped = len;

        bufferLock.lock();

        try {
            if (len > unsafeAvailable()) {
                bytesSkipped = unsafeAvailable();

                nextReadableIndex = 0;
                nextWritableIndex = 0;
                hasElements = false;
            } else {
                nextReadableIndex = nextReadableIndex + len < buffer.length
                        ? nextReadableIndex + len
                        : len - (buffer.length - nextReadableIndex);
            }

        } finally {
            bufferLock.unlock();
        }

        return bytesSkipped;
    }

    @Override
    public int available() {
        bufferLock.lock();

        try {
            return unsafeAvailable();
        } finally {
            bufferLock.unlock();
        }
    }

    @Override
    public int remaining() {
        bufferLock.lock();

        try {
            return unsafeRemaining();
        } finally {
            bufferLock.unlock();
        }
    }

    public int unsafeAvailable() {
        if (nextWritableIndex == nextReadableIndex && hasElements) {
            return buffer.length;
        }

        return nextWritableIndex < nextReadableIndex ? nextWritableIndex + (buffer.length - nextReadableIndex) : nextWritableIndex - nextReadableIndex;
    }

    public int unsafeRemaining() {
        if (nextWritableIndex == nextReadableIndex && hasElements) {
            return 0;
        }

        return nextWritableIndex < nextReadableIndex ? nextReadableIndex - nextWritableIndex : buffer.length - nextWritableIndex + nextReadableIndex;
    }

    private void unsafeGrow(int minLength) {
        final int newSize = buffer.length + buffer.length * (minLength / buffer.length + 1);
        final byte[] newBuffer = new byte[newSize];

        final int read = unsafeGet(newBuffer, 0, newSize);
        buffer = newBuffer;

        nextWritableIndex = read;
        nextReadableIndex = 0;
        hasElements = true;
    }

    private int unsafePut(byte[] b, int off, int len) {
        final int remaining = unsafeRemaining();
        
        if (remaining < len) {
            unsafeGrow(len - remaining);
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

    private int unsafeGet(byte[] b, int off, int len) {
        final int readableLength = unsafeAvailable() > len ? len : unsafeAvailable();

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

    private void unsafePut(byte b) {
        singleByte[0] = b;

        unsafePut(new byte[]{b}, 0, 1);
    }

    private byte unsafeGet() {
        unsafeGet(singleByte, 0, 1);

        return singleByte[0];
    }

    @Override
    public byte get() throws IOException {
        bufferLock.lock();

        try {
            return unsafeAvailable() < 1 ? -1 : unsafeGet();
        } finally {
            bufferLock.unlock();
        }
    }

    @Override
    public int get(byte[] b) throws IOException {
        return get(b, 0, b.length);
    }

    @Override
    public int get(byte[] b, int off, int len) throws IOException {
        bufferLock.lock();

        try {
            return unsafeGet(b, off, len);
        } finally {
            bufferLock.unlock();
        }
    }

    @Override
    public void put(byte b) throws IOException {
        bufferLock.lock();

        try {
            unsafePut(b);
        } finally {
            bufferLock.unlock();
        }
    }

    @Override
    public int put(byte[] b) throws IOException {
        return put(b, 0, b.length);
    }

    @Override
    public int put(byte[] b, int off, int len) throws IOException {
        bufferLock.lock();

        try {
            return unsafePut(b, off, len);
        } finally {
            bufferLock.unlock();
        }
    }

    @Override
    public ByteBuffer copy() {
        bufferLock.lock();

        try {
            final int readableLength = unsafeAvailable();
            final byte[] bufferCopy = new byte[readableLength];

            if (nextReadableIndex + readableLength > buffer.length) {
                final int trimmedLength = buffer.length - nextReadableIndex;

                System.arraycopy(buffer, nextReadableIndex, bufferCopy, 0, trimmedLength);
                System.arraycopy(buffer, 0, bufferCopy, trimmedLength, readableLength - trimmedLength);
            } else {
                System.arraycopy(buffer, nextReadableIndex, bufferCopy, 0, readableLength);
            }

            return new CyclicByteBuffer(bufferCopy, true);
        } finally {
            bufferLock.unlock();
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return copy();
    }
}
