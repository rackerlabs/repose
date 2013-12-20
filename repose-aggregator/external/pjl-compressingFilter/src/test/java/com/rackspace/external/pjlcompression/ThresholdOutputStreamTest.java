/*
 * Copyright 2004 and onwards Sean Owen
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

import com.mockrunner.mock.web.WebMockObjectFactory;
import junit.framework.TestCase;

import javax.servlet.FilterConfig;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Tests {@link CompressingFilter}.
 *
 * @author Sean Owen
 */
public final class ThresholdOutputStreamTest extends TestCase {

	private ByteArrayOutputStream baos;
	private Callback callback;
	private ThresholdOutputStream tos;

	@Override	
	public void setUp() throws Exception {
		super.setUp();
		baos = new ByteArrayOutputStream();
		callback = new Callback();
		WebMockObjectFactory factory = new WebMockObjectFactory();
		FilterConfig filterConfig = factory.getMockFilterConfig();
		CompressingFilterContext context = new CompressingFilterContext(filterConfig);
    CompressingFilterLogger logger = new CompressingFilterLoggerImpl(factory.getMockServletContext(), true, null, false);
		tos = new ThresholdOutputStream(baos,
										CompressingStreamFactory.getFactoryForContentEncoding("gzip"),
										context,
										callback,
                    logger);
	}

	public void testWriteFlush() throws Exception {
		tos.write(0);
		tos.write(new byte[10]);
		tos.write(new byte[10], 0, 5);
		assertEquals(0, baos.size());
		tos.flush();
		// Compresses to 10 bytes; flush() forces compression
		assertEquals(10, baos.size());
		tos.close();
		// Closing adds some additional bytes to the compressed response
		assertEquals(23, baos.size());
		assertFalse(callback.rawStreamCommitted);
		assertTrue(callback.compressingStreamCommitted);
	}

	public void testReset() throws Exception {
		tos.write(new byte[10]);
		assertEquals(0, baos.size());
		tos.reset();
		assertEquals(0, baos.size());
		tos.close();
		assertEquals(0, baos.size());
		assertTrue(callback.rawStreamCommitted);
		assertFalse(callback.compressingStreamCommitted);
	}

	public void testNoCompression() throws Exception {
		byte[] bytes = CompressingFilterResponseTest.SMALL_DOCUMENT.getBytes();
		tos.write(bytes);
		tos.close();
		assertTrue(Arrays.equals(bytes, baos.toByteArray()));
		assertTrue(callback.rawStreamCommitted);
		assertFalse(callback.compressingStreamCommitted);
	}

	public void testForceNoCompression() throws Exception {
		byte[] bytes = CompressingFilterResponseTest.BIG_DOCUMENT.getBytes();
		tos.forceOutputStream1();
		tos.write(bytes);
		tos.close();
		assertTrue(Arrays.equals(bytes, baos.toByteArray()));

		assertTrue(callback.rawStreamCommitted);
		assertFalse(callback.compressingStreamCommitted);
	}

	public void testCompression() throws Exception {
		byte[] bytes = CompressingFilterResponseTest.BIG_DOCUMENT.getBytes();
		tos.write(bytes);
		tos.close();
		assertTrue(baos.size() > 0);
		assertTrue(baos.size() < bytes.length);
		assertFalse(callback.rawStreamCommitted);
		assertTrue(callback.compressingStreamCommitted);
	}

	public void testForceCompression() throws Exception {
		byte[] bytes = CompressingFilterResponseTest.SMALL_DOCUMENT.getBytes();
		tos.switchToOutputStream2();
		for (int i = 0; i < 10; i++) {
			tos.write(bytes);
		}
		tos.close();
		assertTrue(baos.size() > 0);
		assertTrue(baos.size() < 10 * bytes.length);
		assertFalse(callback.rawStreamCommitted);
		assertTrue(callback.compressingStreamCommitted);
	}


	private static final class Callback implements ThresholdOutputStream.BufferCommitmentCallback {
		private boolean rawStreamCommitted;
		private boolean compressingStreamCommitted;
		public void rawStreamCommitted() {
			rawStreamCommitted = true;
		}
		public void compressingStreamCommitted() {
			compressingStreamCommitted = true;
		}
	}

}
