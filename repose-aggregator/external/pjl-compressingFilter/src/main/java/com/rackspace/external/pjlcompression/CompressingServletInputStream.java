/*
 * Copyright 2006 and onwards Sean Owen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rackspace.external.pjlcompression;

import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>Implementation of {@link ServletInputStream} which will decompress data read from it.</p>
 *
 * @author Sean Owen
 * @since 1.6
 */
final class CompressingServletInputStream extends ServletInputStream {

	private final InputStream compressingStream;
	private boolean closed;

	CompressingServletInputStream(InputStream rawStream,
	                              CompressingStreamFactory compressingStreamFactory,
	                              CompressingFilterContext context) throws IOException {
		this.compressingStream =
			compressingStreamFactory.getCompressingStream(rawStream, context).getCompressingInputStream();
	}

	@Override
	public int read() throws IOException {
		checkClosed();
		return compressingStream.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		checkClosed();
		return compressingStream.read(b);
	}

	@Override
	public int read(byte[] b, int offset, int length) throws IOException {
		checkClosed();
		return compressingStream.read(b, offset, length);
	}

	// Leave implementation of readLine() in superclass alone, even if it's not so efficient

	@Override
	public long skip(long n) throws IOException {
		checkClosed();
		return compressingStream.skip(n);
	}

	@Override
	public int available() throws IOException {
		checkClosed();
		return compressingStream.available();
	}

	@Override
	public void close() throws IOException {
		if (!closed) {
			compressingStream.close();
			closed = true;
		}
	}

	@Override
	public synchronized void mark(int readlimit) {
		checkClosed();
		compressingStream.mark(readlimit);
	}

	@Override
	public synchronized void reset() throws IOException {
		checkClosed();
		compressingStream.reset();
	}

	@Override
	public boolean markSupported() {
		checkClosed();
		return compressingStream.markSupported();
	}

	private void checkClosed() {
		if (closed) {
			throw new IllegalStateException("Stream is already closed");
		}
	}

	@Override
	public String toString() {
		return "CompressingServletInputStream";
	}

}
