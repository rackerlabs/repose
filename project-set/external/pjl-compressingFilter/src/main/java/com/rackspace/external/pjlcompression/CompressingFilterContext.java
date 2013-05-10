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

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Encapsulates the {@link CompressingFilter} environment, including configuration and runtime statistics. This object
 * may be conveniently passed around in the code to make this information available.
 *
 * @author Sean Owen
 * @author Peter Bryant
 */
final class CompressingFilterContext {

	private static final int DEFAULT_COMPRESSION_THRESHOLD = 1024;
  private static final Pattern COMMA = Pattern.compile(",");

  private final boolean debug;
	private final CompressingFilterLogger logger;
	private final int compressionThreshold;
	private final ServletContext servletContext;
	private final CompressingFilterStats stats;
	private final boolean includeContentTypes;
	private final Collection<String> contentTypes;
	// Thanks to Peter Bryant for suggesting this functionality:
	private final boolean includePathPatterns;
	private final Collection<Pattern> pathPatterns;
	// Thanks to reimerl for proposing this + sample code
	private final boolean includeUserAgentPatterns;
	private final Collection<Pattern> userAgentPatterns;

	CompressingFilterContext(FilterConfig filterConfig) throws ServletException {

		assert filterConfig != null;

		debug = readBooleanValue(filterConfig, "debug");

		String javaUtilDelegateName = filterConfig.getInitParameter("javaUtilLogger");
		if (javaUtilDelegateName != null) {
			logger = new CompressingFilterLoggerImpl(filterConfig.getServletContext(),
			                                         debug,
			                                         javaUtilDelegateName,
			                                         true);
		} else {
			String jakartaCommonsDelegateName =
				filterConfig.getInitParameter("jakartaCommonsLogger");
      logger = new CompressingFilterLoggerImpl(filterConfig.getServletContext(),
                                               debug,
                                               jakartaCommonsDelegateName,
                                               false);
		}

		logger.logDebug("Debug logging statements are enabled");

		compressionThreshold = readCompressionThresholdValue(filterConfig);
		if (logger.isDebug()) {
			logger.logDebug("Using compressing threshold: " + compressionThreshold);
		}

		servletContext = filterConfig.getServletContext();
		assert this.servletContext != null;

		if (readBooleanValue(filterConfig, "statsEnabled")) {
			stats = new CompressingFilterStats();
			ensureStatsInContext();
			logger.logDebug("Stats are enabled");
		} else {
			stats = null;
			logger.logDebug("Stats are disabled");
		}

		String includeContentTypesString = filterConfig.getInitParameter("includeContentTypes");
		String excludeContentTypesString = filterConfig.getInitParameter("excludeContentTypes");
		if (includeContentTypesString != null && excludeContentTypesString != null) {
			throw new IllegalArgumentException("Can't specify both includeContentTypes and excludeContentTypes");
		}

		if (includeContentTypesString == null) {
			includeContentTypes = false;
			contentTypes = parseContentTypes(excludeContentTypesString);
		} else {
			includeContentTypes = true;
			contentTypes = parseContentTypes(includeContentTypesString);
		}

		if (!contentTypes.isEmpty()) {
			logger.logDebug("Filter will " + (includeContentTypes ? "include" : "exclude") +
							" only these content types: " + contentTypes);
		}

		String includePathPatternsString = filterConfig.getInitParameter("includePathPatterns");
		String excludePathPatternsString = filterConfig.getInitParameter("excludePathPatterns");
		if (includePathPatternsString != null && excludePathPatternsString != null) {
			throw new IllegalArgumentException("Can't specify both includePathPatterns and excludePathPatterns");
		}

		if (includePathPatternsString == null) {
			includePathPatterns = false;
			pathPatterns = parsePatterns(excludePathPatternsString);
		} else {
			includePathPatterns = true;
			pathPatterns = parsePatterns(includePathPatternsString);
		}

		if (!pathPatterns.isEmpty() && logger.isDebug()) {
			logger.logDebug("Filter will " + (includePathPatterns ? "include" : "exclude") +
			                " only these file patterns: " + pathPatterns);
		}

		String includeUserAgentPatternsString = filterConfig.getInitParameter("includeUserAgentPatterns");
		String excludeUserAgentPatternsString = filterConfig.getInitParameter("excludeUserAgentPatterns");
		if (includeUserAgentPatternsString != null && excludeUserAgentPatternsString != null) {
			throw new IllegalArgumentException(
				"Can't specify both includeUserAgentPatterns and excludeUserAgentPatterns");
		}

		if (includeUserAgentPatternsString == null) {
			includeUserAgentPatterns = false;
			userAgentPatterns = parsePatterns(excludeUserAgentPatternsString);
		} else {
			includeUserAgentPatterns = true;
			userAgentPatterns = parsePatterns(includeUserAgentPatternsString);
		}

		if (!userAgentPatterns.isEmpty() && logger.isDebug()) {
			logger.logDebug("Filter will " + (includeUserAgentPatterns ? "include" : "exclude") +
			                " only these User-Agent patterns: " + userAgentPatterns);
		}

	}

	boolean isDebug() {
		return debug;
	}

	CompressingFilterLogger getLogger() {
		assert logger != null;
		return logger;
	}

	int getCompressionThreshold() {
		return compressionThreshold;
	}

	CompressingFilterStats getStats() {
		if (stats == null) {
			throw new IllegalStateException("Stats are not enabled");
		}
		ensureStatsInContext();
		return stats;
	}

	boolean isStatsEnabled() {
		return stats != null;
	}

	boolean isIncludeContentTypes() {
		return includeContentTypes;
	}

	Collection<String> getContentTypes() {
		return contentTypes;
	}

	boolean isIncludePathPatterns() {
		return includePathPatterns;
	}

	Iterable<Pattern> getPathPatterns() {
		return pathPatterns;
	}

	boolean isIncludeUserAgentPatterns() {
		return includeUserAgentPatterns;
	}

	Iterable<Pattern> getUserAgentPatterns() {
		return userAgentPatterns;
	}

	@Override
	public String toString() {
		return "CompressingFilterContext";
	}

	private void ensureStatsInContext() {
		assert servletContext != null;
		if (servletContext.getAttribute(CompressingFilterStats.STATS_KEY) == null) {
			servletContext.setAttribute(CompressingFilterStats.STATS_KEY, stats);
		}
	}

	private static boolean readBooleanValue(FilterConfig filterConfig, String parameter) {
		return Boolean.valueOf(filterConfig.getInitParameter(parameter));
	}

	private static int readCompressionThresholdValue(FilterConfig filterConfig) throws ServletException {
		String compressionThresholdString = filterConfig.getInitParameter("compressionThreshold");
		int value;
		if (compressionThresholdString != null) {
			try {
				value = Integer.parseInt(compressionThresholdString);
			} catch (NumberFormatException nfe) {
				throw new ServletException("Invalid compression threshold: " + compressionThresholdString, nfe);
			}
			if (value < 0) {
				throw new ServletException("Compression threshold cannot be negative");
			}
		} else {
			value = DEFAULT_COMPRESSION_THRESHOLD;
		}
		return value;
	}

	private static Collection<String> parseContentTypes(String contentTypesString) {
		if (contentTypesString == null) {
			return Collections.emptyList();
		}
		List<String> contentTypes = new ArrayList<String>(5);
		for (String contentType : COMMA.split(contentTypesString)) {
			if (contentType.length() > 0) {
				contentTypes.add(contentType);
			}
		}
		return Collections.unmodifiableList(contentTypes);
	}

	private static Collection<Pattern> parsePatterns(String patternsString) {
		if (patternsString == null) {
			return Collections.emptyList();
		}
		List<Pattern> patterns = new ArrayList<Pattern>(5);
		for (String pattern : COMMA.split(patternsString)) {
			if (pattern.length() > 0) {
				patterns.add(Pattern.compile(pattern));
			}
		}
		return Collections.unmodifiableList(patterns);
	}

}
