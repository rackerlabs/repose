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
import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Tests {@link CompressingFilter} compressing responses.
 *
 * @author Sean Owen
 */
public final class CompressingFilterResponseTest extends TestCase {

	private static final String TEST_ENCODING = "ISO-8859-1";

	static final String SMALL_DOCUMENT = "Test";
	static final String BIG_DOCUMENT;
	private static final String EMPTY = "";

	static {
		// Make up a random, but repeatable String
		Random r = new Random(0xDEADBEEFL);
		byte[] bytes = new byte[10000];
		r.nextBytes(bytes);
		String temp = null;
		try {
			temp = new String(bytes, TEST_ENCODING);
		} catch (UnsupportedEncodingException uee) {
			// can't happen
		}
		BIG_DOCUMENT = temp;
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
		config.setInitParameter("excludePathPatterns", ".*badpath.*,whocares");
		config.setInitParameter("excludeContentTypes", "text/badtype,whatever");
		config.setInitParameter("excludeUserAgentPatterns", "Nokia.*");
		module = new ServletTestModule(factory);
		module.addFilter(new CompressingFilter(), true);
		module.setDoChain(true);
		factory.getMockResponse().setCharacterEncoding(TEST_ENCODING);
	}

	@Override
	public void tearDown() throws Exception {
		factory = null;
		module = null;
		super.tearDown();
	}

	public void testSmallOutput() throws Exception {
		verifyOutput(SMALL_DOCUMENT, false);
	}

	public void testBigOutput() throws Exception {
		verifyOutput(BIG_DOCUMENT, true);

		CompressingFilterStats stats = (CompressingFilterStats)
			factory.getMockServletContext().getAttribute(CompressingFilterStats.STATS_KEY);
		assertNotNull(stats);

		assertEquals(0, stats.getNumRequestsCompressed());
		assertEquals(1, stats.getTotalRequestsNotCompressed());
		assertEquals(0.0, stats.getRequestAverageCompressionRatio());
		assertEquals(0L, stats.getRequestCompressedBytes());
		assertEquals(0L, stats.getRequestInputBytes());

		assertEquals(1, stats.getNumResponsesCompressed());
		assertEquals(0, stats.getTotalResponsesNotCompressed());
		assertEquals(0.9977, stats.getResponseAverageCompressionRatio(), 0.0001);
		assertEquals(10023L, stats.getResponseCompressedBytes());
		assertEquals(10000L, stats.getResponseInputBytes());
	}

	public void testAlreadyApplied() throws Exception {
		// add the filter again
		module.addFilter(new CompressingFilter(), true);
		verifyOutput(BIG_DOCUMENT, true);
	}

	public void testForceEncoding() throws Exception {
		// force no-compression compression for a big response
		module.setRequestAttribute(CompressingFilter.FORCE_ENCODING_KEY, "identity");
		verifyOutput(BIG_DOCUMENT, false);
	}

  public void testNoTransform() throws Exception {
		module.setServlet(new HttpServlet() {
			@Override
			public void doGet(HttpServletRequest request,
			                  HttpServletResponse response) throws IOException {
				response.setHeader("Cache-Control", "no-transform");
				response.getWriter().print(BIG_DOCUMENT);
			}
		});
		verifyOutput(BIG_DOCUMENT, false);
	}

	public void testExcludePathPatterns1() throws Exception {
		MockHttpServletRequest request = factory.getMockRequest();
		request.setRequestURI("/some/goodpath/index.html");
		verifyOutput(BIG_DOCUMENT, true);
	}

	public void testExcludePathPatterns2() throws Exception {
		MockHttpServletRequest request = factory.getMockRequest();
		request.setRequestURI("/some/badpath/index.html");
		verifyOutput(BIG_DOCUMENT, false);
	}

	public void testExcludeUserAgentPatterns1() throws Exception {
		MockHttpServletRequest request = factory.getMockRequest();
		request.setHeader("User-Agent", "MSIE5");
		verifyOutput(BIG_DOCUMENT, true);
	}

	public void testExcludeUserAgentPatterns2() throws Exception {
		MockHttpServletRequest request = factory.getMockRequest();
		request.setHeader("User-Agent", "Nokia6820");
		verifyOutput(BIG_DOCUMENT, false);
	}

	public void testExcludeContentTypes1() throws Exception {
		module.setServlet(new HttpServlet() {
			@Override
			public void doGet(HttpServletRequest request,
			                  HttpServletResponse response) throws IOException {
				response.setContentType("text/badtype; otherstuff");
				response.getWriter().print(BIG_DOCUMENT);
			}
		});
		verifyOutput(BIG_DOCUMENT, false);
	}

	public void testExcludeContentTypes2() throws Exception {
		module.setServlet(new HttpServlet() {
			@Override
			public void doGet(HttpServletRequest request,
			                  HttpServletResponse response) throws IOException {
				response.setContentType("text/goodtype; otherstuff");
				response.getWriter().print(BIG_DOCUMENT);
			}
		});
		verifyOutput(BIG_DOCUMENT, true);
	}

	public void testRedirect() throws Exception {
		module.setServlet(new HttpServlet() {
			@Override
			public void doGet(HttpServletRequest request,
			                  HttpServletResponse response) throws IOException {
				response.sendRedirect("http://www.google.com/");
			}
		});

		module.doGet();

		MockHttpServletResponse response = factory.getMockResponse();

		// Mockrunner doesn't set status 302:
		//assertEquals(302, response.getStatusCode());
		assertTrue(response.wasRedirectSent());
		assertFalse(response.wasErrorSent());
    assertTrue(response.containsHeader(CompressingFilter.VARY_HEADER));
	}

	public void testFlush() {
		module.setServlet(new HttpServlet() {
			@Override
			public void doGet(HttpServletRequest request,
			                  HttpServletResponse response) throws IOException {
				response.getWriter().print(SMALL_DOCUMENT);
				response.flushBuffer();
				response.getWriter().print(SMALL_DOCUMENT);
			}
		});

		module.doGet();

		MockHttpServletResponse response = factory.getMockResponse();

		assertEquals(HttpServletResponse.SC_OK, response.getStatusCode());
		assertFalse(response.wasRedirectSent());
		assertFalse(response.wasErrorSent());
    assertTrue(response.containsHeader(CompressingFilter.VARY_HEADER));
	}

	public void testClose() {
		module.setServlet(new HttpServlet() {
			@Override
			public void doGet(HttpServletRequest request,
			                  HttpServletResponse response) throws IOException {
				response.getWriter().print(SMALL_DOCUMENT);
				response.getWriter().close();
			}
		});

		module.doGet();

		MockHttpServletResponse response = factory.getMockResponse();

		assertEquals(HttpServletResponse.SC_OK, response.getStatusCode());
		assertFalse(response.wasRedirectSent());
		assertFalse(response.wasErrorSent());
		assertEquals(SMALL_DOCUMENT, module.getOutput());
		assertNull(module.getRequestAttribute(CompressingFilter.COMPRESSED_KEY));
    assertTrue(response.containsHeader(CompressingFilter.VARY_HEADER));
	}

	public void testSpuriousFlushClose() {
		module.setServlet(new HttpServlet() {
			@Override
			public void doGet(HttpServletRequest request,
			                  HttpServletResponse response) throws IOException {
				response.getWriter().print(SMALL_DOCUMENT);
				response.getWriter().close();
				response.getWriter().flush();
				response.getWriter().close();
			}
		});

		module.doGet();

		MockHttpServletResponse response = factory.getMockResponse();

		assertEquals(HttpServletResponse.SC_OK, response.getStatusCode());
		assertFalse(response.wasRedirectSent());
		assertFalse(response.wasErrorSent());
		assertEquals(SMALL_DOCUMENT, module.getOutput());
		assertNull(module.getRequestAttribute(CompressingFilter.COMPRESSED_KEY));
    assertTrue(response.containsHeader(CompressingFilter.VARY_HEADER));
	}

	public void testNoGzipOutput() {
		doTestNoOutput();
	}

	public void testNoZipOutput() {
		MockHttpServletRequest request = factory.getMockRequest();
		request.addHeader("Content-Encoding", "compress");
		doTestNoOutput();
	}

	private void doTestNoOutput() {
		module.setServlet(new HttpServlet() {
			@Override
			public void doGet(HttpServletRequest request,
			                  HttpServletResponse response) throws IOException {
				response.getWriter().close();
			}
		});

		module.doGet();

		MockHttpServletResponse response = factory.getMockResponse();

		assertEquals(HttpServletResponse.SC_OK, response.getStatusCode());
		assertFalse(response.wasRedirectSent());
		assertFalse(response.wasErrorSent());
		assertEquals(EMPTY, module.getOutput());
		assertNull(module.getRequestAttribute(CompressingFilter.COMPRESSED_KEY));

		assertFalse(response.containsHeader("Content-Encoding"));
		assertFalse(response.containsHeader("X-Compressed-By"));
    assertTrue(response.containsHeader(CompressingFilter.VARY_HEADER));
	}

	private void verifyOutput(final String output, boolean shouldCompress) throws IOException {
		if (module.getServlet() == null) {
			module.setServlet(new HttpServlet() {
				@Override
				public void doGet(HttpServletRequest request,
								  HttpServletResponse response) throws IOException {
          response.setHeader("ETag", String.valueOf(output.hashCode())); // Fake ETag
					response.getWriter().print(output);
				}
			});
		}
		MockHttpServletRequest request = factory.getMockRequest();
		request.addHeader("Accept-Encoding", "deflate,gzip");

		module.doGet();

		MockHttpServletResponse response = factory.getMockResponse();

		assertEquals(HttpServletResponse.SC_OK, response.getStatusCode());
		assertFalse(response.wasRedirectSent());
		assertFalse(response.wasErrorSent());

		if (shouldCompress) {
      assertTrue(response.containsHeader("Vary"));
			byte[] expectedBytes = getCompressedOutput(output.getBytes(TEST_ENCODING));
			// Since ServletTestModule makes a String out of the output according to ISO-8859-1 encoding,
			// do the same for expected bytes and then compare. Don't use assertEquals(); you'll just see
			// a bunch of binary garbage if the results differ
			String moduleOutput = module.getOutput();
			assertFalse(output.equals(moduleOutput));
			String expectedString = new String(expectedBytes, TEST_ENCODING);
			assertEquals(expectedString, moduleOutput);
			assertEquals(Boolean.TRUE, module.getRequestAttribute(CompressingFilter.COMPRESSED_KEY));

			assertTrue(response.containsHeader("Content-Encoding"));
			assertTrue(response.containsHeader("X-Compressed-By"));
      assertTrue(!response.containsHeader("ETag") || response.getHeader("ETag").endsWith("-gzip"));
		} else {
			assertEquals(output, module.getOutput());
			assertNull(module.getRequestAttribute(CompressingFilter.COMPRESSED_KEY));
		}


	}

	private static byte[] getCompressedOutput(byte[] output) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DeflaterOutputStream gzipOut = new GZIPOutputStream(baos);
		gzipOut.write(output);
		gzipOut.finish();
		gzipOut.close();
		return baos.toByteArray();
	}

}
