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

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Implementation of {@link HttpServletResponse} which will optionally compress data written to the response.
 *
 * @author Sean Owen
 */
final class CompressingHttpServletResponse extends HttpServletResponseWrapper {

	static final String ACCEPT_ENCODING_HEADER = "Accept-Encoding";
  private static final String CACHE_CONTROL_HEADER = "Cache-Control";
	static final String CONTENT_ENCODING_HEADER = "Content-Encoding";
	private static final String CONTENT_LENGTH_HEADER = "Content-Length";
	private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String ETAG_HEADER = "ETag";
	private static final String X_COMPRESSED_BY_HEADER = "X-Compressed-By";

  private static final String[] UNALLOWED_HEADERS =
      {CACHE_CONTROL_HEADER, CONTENT_LENGTH_HEADER, CONTENT_ENCODING_HEADER, ETAG_HEADER, X_COMPRESSED_BY_HEADER};

	private static final String COMPRESSED_BY_VALUE = CompressingFilter.VERSION_STRING;


	private final HttpServletResponse httpResponse;

	private final CompressingFilterContext context;
	private final CompressingFilterLogger logger;

	private final String compressedContentEncoding;
	private final CompressingStreamFactory compressingStreamFactory;
	private CompressingServletOutputStream compressingSOS;

	private PrintWriter printWriter;
	private boolean isGetOutputStreamCalled;
	private boolean isGetWriterCalled;

	private boolean compressing;

	private long savedContentLength;
	private boolean savedContentLengthSet;
	private String savedContentEncoding;
  private String savedETag;
	private boolean contentTypeOK;
  private boolean noTransformSet;


  CompressingHttpServletResponse(HttpServletResponse httpResponse,
	                               CompressingStreamFactory compressingStreamFactory,
	                               String contentEncoding,
	                               CompressingFilterContext context) {
		super(httpResponse);
		this.httpResponse = httpResponse;
		this.compressedContentEncoding = contentEncoding;
		compressing = false;
		logger = context.getLogger();
		this.compressingStreamFactory = compressingStreamFactory;
		this.context = context;
		contentTypeOK = true;
 	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (isGetWriterCalled) {
			throw new IllegalStateException("getWriter() has already been called");
		}
		isGetOutputStreamCalled = true;
		return getCompressingServletOutputStream();
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (isGetOutputStreamCalled) {
			throw new IllegalStateException("getCompressingOutputStream() has already been called");
		}
		isGetWriterCalled = true;
		if (printWriter == null) {
			printWriter = new PrintWriter(new OutputStreamWriter(getCompressingServletOutputStream(),
			                                                     getCharacterEncoding()),
			                              true);
		}
		return printWriter;
	}

  /**
   * @see #setHeader(String, String)
   */
	@Override
	public void addHeader(String name, String value) {
    if (CACHE_CONTROL_HEADER.equalsIgnoreCase(name)) {
      httpResponse.addHeader(CACHE_CONTROL_HEADER, value);
      if (value.contains("no-transform")) {
        logger.logDebug("Aborting compression due to no-transform directive");
        noTransformSet = true;
        maybeAbortCompression();
      }
    } else if (CONTENT_ENCODING_HEADER.equalsIgnoreCase(name)) {
			savedContentEncoding = value;
      if (!isCompressableEncoding(value)) {
        maybeAbortCompression();
      }
		} else if (CONTENT_LENGTH_HEADER.equalsIgnoreCase(name)) {
			// Not setContentLength(); we want to potentially accommodate a long value here
			doSetContentLength(Long.parseLong(value));
		} else if (CONTENT_TYPE_HEADER.equalsIgnoreCase(name)) {
			setContentType(value);
    } else if (ETAG_HEADER.equalsIgnoreCase(name)) {
      // Later, when the container perhaps sets ETag, try to set a different value (just by
      // appending "-gzip" for instance) to reflect that the body is not the same as the uncompressed
      // version. Otherwise caches may incorrectly return the compressed version to a
      // client that doesn't want it
      savedETag = value;
      setETagHeader();
		} else if (isAllowedHeader(name)) {
			httpResponse.addHeader(name, value);
		}
	}

  /**
   * @see #setIntHeader(String, int)
   */
	@Override
	public void addIntHeader(String name, int value) {
		if (CONTENT_LENGTH_HEADER.equalsIgnoreCase(name)) {
			setContentLength(value);
    } else if (ETAG_HEADER.equalsIgnoreCase(name)) {
      // Later, when the container perhaps sets ETag, try to set a different value (just by
      // appending "-gzip" for instance) to reflect that the body is not the same as the uncompressed
      // version. Otherwise caches may incorrectly return the compressed version to a
      // client that doesn't want it
      savedETag = String.valueOf(value);
      setETagHeader();
		} else if (isAllowedHeader(name)) {
			httpResponse.addIntHeader(name, value);
		}
	}

	@Override
	public void addDateHeader(String name, long value) {
		if (isAllowedHeader(name)) {
			httpResponse.addDateHeader(name, value);
		}
	}

  /**
   * @see #addHeader(String, String)
   */
	@Override
	public void setHeader(String name, String value) {
    if (CACHE_CONTROL_HEADER.equalsIgnoreCase(name)) {
      httpResponse.setHeader(CACHE_CONTROL_HEADER, value);
      if (value.contains("no-transform")) {
        logger.logDebug("Aborting compression due to no-transform directive");
        noTransformSet = true;
        maybeAbortCompression();
      }
    } else if (CONTENT_ENCODING_HEADER.equalsIgnoreCase(name)) {
			savedContentEncoding = value;
      if (!isCompressableEncoding(value)) {
        maybeAbortCompression();
      }
		} else if (CONTENT_LENGTH_HEADER.equalsIgnoreCase(name)) {
			// Not setContentLength(); we want to potentially accommodate a long value here
			doSetContentLength(Long.parseLong(value));
		} else if (CONTENT_TYPE_HEADER.equalsIgnoreCase(name)) {
			setContentType(value);
    } else if (ETAG_HEADER.equalsIgnoreCase(name)) {
      // Later, when the container perhaps sets ETag, try to set a different value (just by
      // appending "-gzip" for instance) to reflect that the body is not the same as the uncompressed
      // version. Otherwise caches may incorrectly return the compressed version to a
      // client that doesn't want it
      savedETag = value;
      setETagHeader();
		} else if (isAllowedHeader(name)) {
			httpResponse.setHeader(name, value);
		}
	}

  private void maybeAbortCompression() {
    if (compressingSOS != null) {
      try {
        compressingSOS.abortCompression();
      } catch (IOException ioe) {
        // Can't throw this, hmm...
        logger.log("Unexpected error while aborting compression", ioe);
      }
    }
  }

  private void setETagHeader() {
    if (savedETag != null) {
      if (compressing) {
        httpResponse.setHeader(ETAG_HEADER, savedETag + '-' + compressedContentEncoding);
      } else {
        httpResponse.setHeader(ETAG_HEADER, savedETag);
      }
    }
  }

  /**
   * @see #addIntHeader(String, int)
   */
	@Override
	public void setIntHeader(String name, int value) {
		if (CONTENT_LENGTH_HEADER.equalsIgnoreCase(name)) {
			setContentLength(value);
    } else if (ETAG_HEADER.equalsIgnoreCase(name)) {
      // Later, when the container perhaps sets ETag, try to set a different value (just by
      // appending "-gzip" for instance) to reflect that the body is not the same as the uncompressed
      // version. Otherwise caches may incorrectly return the compressed version to a
      // client that doesn't want it
      savedETag = String.valueOf(value);
      setETagHeader();
		} else if (isAllowedHeader(name)) {
			httpResponse.setIntHeader(name, value);
		}
	}

	@Override
	public void setDateHeader(String name, long value) {
		if (isAllowedHeader(name)) {
			httpResponse.setDateHeader(name, value);
		}
	}

	@Override
	public void flushBuffer() {
		flushWriter(); // make sure nothing is buffered in the writer, if applicable
		if (compressingSOS != null) {
			compressingSOS.flush();
		}
	}

	@Override
	public void reset() {
		flushWriter(); // make sure nothing is buffered in the writer, if applicable
		if (compressingSOS != null) {
			compressingSOS.reset();
		}
		httpResponse.reset();
		if (compressing) {
			setCompressionResponseHeaders();
		} else {
      setNonCompressionResponseHeaders();
    }
	}

	@Override
	public void resetBuffer() {
		flushWriter(); // make sure nothing is buffered in the writer, if applicable
		if (compressingSOS != null) {
			compressingSOS.reset();
		}
		httpResponse.resetBuffer();
	}

	@Override
	public void setContentLength(int contentLength) {
		// Internally we want to be able to handle a long contentLength, but the ServletResponse method
		// is declared to take an int. So we delegate to a private version that handles a long, and reuse
		// that version elsewhere here.
		doSetContentLength((long) contentLength);
	}

	private void doSetContentLength(long contentLength) {
		if (compressing) {
			// do nothing -- caller-supplied content length is not meaningful
			logger.logDebug("Ignoring application-specified content length since response is compressed");
		} else {
			savedContentLength = contentLength;
			savedContentLengthSet = true;
			logger.logDebug("Saving application-specified content length for later: " + contentLength);
      if (compressingSOS != null && compressingSOS.isAborted()) {
        httpResponse.setHeader(CONTENT_LENGTH_HEADER, String.valueOf(contentLength));        
      }
		}
	}

	@Override
	public void setContentType(String contentType) {
		contentTypeOK = isCompressableContentType(contentType);
    httpResponse.setContentType(contentType);
    if (!contentTypeOK && compressingSOS != null) {
      logger.logDebug("Aborting compression since Content-Type is excluded: " + contentType);
      maybeAbortCompression();
    }
	}

	@Override
	public String toString() {
		return "CompressingHttpServletResponse[compressing: " + compressing + ']';
	}

	boolean isCompressing() {
		return compressing;
	}

	void close() throws IOException {
		if (compressingSOS != null && !compressingSOS.isClosed()) {
			compressingSOS.close();
		}
	}

	private void setCompressionResponseHeaders() {
		logger.logDebug("Setting compression-related headers");
    String fullContentEncodingHeader = savedContentEncoding == null ?
                                       compressedContentEncoding :
                                       savedContentEncoding + ',' + compressedContentEncoding;
		httpResponse.setHeader(CONTENT_ENCODING_HEADER, fullContentEncodingHeader);
    setETagHeader();    
		if (context.isDebug()) {
			httpResponse.setHeader(X_COMPRESSED_BY_HEADER, COMPRESSED_BY_VALUE);
		}
	}

  private void setNonCompressionResponseHeaders() {
    if (savedContentLengthSet) {
		  httpResponse.setHeader(CONTENT_LENGTH_HEADER, String.valueOf(savedContentLength));
		}
		if (savedContentEncoding != null) {
			httpResponse.setHeader(CONTENT_ENCODING_HEADER, savedContentEncoding);
		}
  }

	void rawStreamCommitted() {
		assert !compressing;
		logger.logDebug("Committing response without compression");
		setNonCompressionResponseHeaders();
	}

	void switchToCompression() {
		assert !compressing;
		logger.logDebug("Switching to compression in the response");
		compressing = true;
		setCompressionResponseHeaders();
	}

	/**
	 * <p>Returns true if and only if the named HTTP header may be set directly by the
   * application, as some headers must be handled specially. null is allowed, though it
   * setting a header named null will probably generate an exception from
	 * the underlying {@link HttpServletResponse}. {@link #CONTENT_LENGTH_HEADER},
   * {@link #CONTENT_ENCODING_HEADER} and {@link #X_COMPRESSED_BY_HEADER} are not allowed.</p>
	 *
	 * @param header name of HTTP header
	 * @return true if and only if header can be set directly by application
	 */
	private boolean isAllowedHeader(String header) {
		boolean unallowed = header != null && equalsIgnoreCaseAny(header, UNALLOWED_HEADERS);
		if (unallowed && logger.isDebug()) {
			logger.logDebug("Header '" + header + "' cannot be set by application");
		}
		return !unallowed;
	}

  private static boolean equalsIgnoreCaseAny(String a, String... others) {
    for (String other : others) {
      if (a.equalsIgnoreCase(other)) {
        return true;
      }
    }
    return false;
  }

	private void flushWriter() {
		if (printWriter != null) {
			printWriter.flush();
		}
	}

	/**
   * <p>Checks to see if the given content type should be compressed. If the content type indicates it
   * is already a compressed format (e.g. contains "gzip") then this wil return {@code false}.</p>
   *
   * <p>Otherwise this checks against the
   * {@code includeContentTypes} and {@code excludeContentTypes} filter init
   * parameters; if the former is set and the given content type is in that parameter's
   * list, or if the latter is set and the content type
   * is not in that list, then this method returns {@code true}.</p>
   *
   * @param contentType content type of response
   * @return true if and only if the given content type should be compressed
   */
	private boolean isCompressableContentType(String contentType) {
		String contentTypeOnly = contentType;
		if (contentType != null) {
			int semicolonIndex = contentType.indexOf((int) ';');
			if (semicolonIndex >= 0) {
				contentTypeOnly = contentType.substring(0, semicolonIndex);
			}
		} else {
      return true;
    }

    for (String compressionEncoding : CompressingStreamFactory.ALL_COMPRESSION_ENCODINGS) {
      if (contentTypeOnly.contains(compressionEncoding)) {
        return false;
      }
    }
		boolean isContained = context.getContentTypes().contains(contentTypeOnly);
		return context.isIncludeContentTypes() ? isContained : !isContained;
	}

  private static boolean isCompressableEncoding(String encoding) {
    if (encoding == null) {
      return true;
    }
    for (String compressionEncoding : CompressingStreamFactory.ALL_COMPRESSION_ENCODINGS) {
      if (encoding.equals(compressionEncoding)) {
        return false;
      }
    }
    return true;
  }

	private CompressingServletOutputStream getCompressingServletOutputStream() throws IOException {
		if (compressingSOS == null) {
			compressingSOS =
			    new CompressingServletOutputStream(httpResponse.getOutputStream(),
			                                       compressingStreamFactory,
			                                       this,
			                                       context,
                                             logger);
		}

		if (!compressingSOS.isClosed()) {
			// Do we already know we don't want to compress?
			// Is there a reason we know compression will be used, already?
			if (mustNotCompress()) {
				compressingSOS.abortCompression();
			}
		}

		return compressingSOS;
	}

	private boolean mustNotCompress() {
     contentTypeOK = isCompressableContentType(this.httpResponse.getHeader("content-type"));
		if (!contentTypeOK) {
			logger.logDebug("Will not compress since configuration excludes this content type");
			return true;
		}
		if (savedContentLengthSet &&
		    savedContentLength < (long) context.getCompressionThreshold()) {
			logger.logDebug("Will not compress since page has set a content length which is less than " +
			                "the compression threshold: " + savedContentLength);
			return true;
		}
    if (noTransformSet) {
      logger.logDebug("Will not compress since no-transform was specified");
      return true;
    }
    return !isCompressableEncoding(savedContentEncoding);
  }

}
