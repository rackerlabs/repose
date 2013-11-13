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

import com.mockrunner.mock.web.MockFilterConfig;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import com.mockrunner.mock.web.WebMockObjectFactory;
import com.mockrunner.servlet.ServletTestModule;
import junit.framework.TestCase;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Tests {@link CompressingFilter} compressed requests.
 *
 * @author Sean Owen
 * @since 1.6
 */
public final class CompressingFilterRequestTest extends TestCase {

	private static final byte[] BIG_DOCUMENT;
	static {
		// Make up a random, but repeatable String
		Random r = new Random(0xDEADBEEFL);
		BIG_DOCUMENT = new byte[10000];
		r.nextBytes(BIG_DOCUMENT);
	}

	private WebMockObjectFactory factory;
	private ServletTestModule module;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		factory = new WebMockObjectFactory();
		MockFilterConfig config = factory.getMockFilterConfig();
		config.setInitParameter("debug", "true");
		config.setInitParameter("statsEnabled", "true");
		module = new ServletTestModule(factory);
		module.addFilter(new CompressingFilter(), true);
		module.setDoChain(true);
	}

	@Override
	public void tearDown() throws Exception {
		factory = null;
		module = null;
		super.tearDown();
	}


	public void testBigOutput() throws Exception {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
		if (module.getServlet() == null) {
			module.setServlet(new HttpServlet() {
				@Override
				public void doGet(HttpServletRequest request,
				                  HttpServletResponse response) throws IOException {
					InputStream sis = request.getInputStream();
					byte[] buffer = new byte[1024];
					int bytesRead;
					while ((bytesRead = sis.read(buffer)) > 0) {
						baos.write(buffer, 0, bytesRead);
					}
					baos.close();
				}
			});
		}
		MockHttpServletRequest request = factory.getMockRequest();
		request.addHeader("Content-Encoding", "gzip");
		byte[] compressedBigDoc = getCompressedOutput(BIG_DOCUMENT);
		request.setBodyContent(compressedBigDoc);

		module.doGet();

		MockHttpServletResponse response = factory.getMockResponse();
		assertEquals(HttpServletResponse.SC_OK, response.getStatusCode());
		assertFalse(response.wasRedirectSent());
		assertFalse(response.wasErrorSent());

		assertTrue(Arrays.equals(BIG_DOCUMENT, baos.toByteArray()));

		CompressingFilterStats stats = (CompressingFilterStats)
			factory.getMockServletContext().getAttribute(CompressingFilterStats.STATS_KEY);
		assertNotNull(stats);

		assertEquals(1, stats.getNumRequestsCompressed());
		assertEquals(0, stats.getTotalRequestsNotCompressed());
		assertEquals((double) BIG_DOCUMENT.length / (double) compressedBigDoc.length, stats.getRequestAverageCompressionRatio());
		assertEquals((long) compressedBigDoc.length, stats.getRequestCompressedBytes());
		assertEquals((long) BIG_DOCUMENT.length, stats.getRequestInputBytes());

		assertEquals(0, stats.getNumResponsesCompressed());
		assertEquals(1, stats.getTotalResponsesNotCompressed());
		assertEquals(0.0, stats.getResponseAverageCompressionRatio());
		assertEquals(0L, stats.getResponseCompressedBytes());
		assertEquals(0L, stats.getResponseInputBytes());
	}

	private static byte[] getCompressedOutput(byte[] output) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DeflaterOutputStream gzipOut = new GZIPOutputStream(baos);
		gzipOut.write(output);
		gzipOut.finish();
		gzipOut.close();
		baos.close();
		return baos.toByteArray();
	}

}
