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

import java.io.Serializable;

/**
 * <p>This class provides runtime statistics on the performance of {@link CompressingFilter}. If stats are enabled, then
 * an instance of this object will be available in the servlet context under the key {@link #STATS_KEY}. It can be
 * retrieved and used like so:</p> <p/>
 * <pre>
 * ServletContext ctx = ...;
 * // in a JSP, "ctx" is already available as the "application" variable
 * CompressingFilterStats stats = (CompressingFilterStats) ctx.getAttribute(CompressingFilterStats.STATS_KEY);
 * double ratio = stats.getAverageCompressionRatio();
 * ...
 * </pre>
 *
 * @author Sean Owen
 * @since 1.1
 */
public final class CompressingFilterStats implements Serializable {

	private static final long serialVersionUID = -2246829834191152845L;

	/**
	 * Key under which a {@link CompressingFilterStats} object can be found in the servlet context.
	 */
	public static final String STATS_KEY = "com.planetj.servlet.filter.compression.CompressingFilterStats";

	/** @serial */
	private int numResponsesCompressed;
	/** @serial */
	private int totalResponsesNotCompressed;
	/** @serial */
	private long responseInputBytes;
	/** @serial */
	private long responseCompressedBytes;
	/** @serial */
	private int numRequestsCompressed;
	/** @serial */
	private int totalRequestsNotCompressed;
	/** @serial */
	private long requestInputBytes;
	/** @serial */
	private long requestCompressedBytes;
	/** @serial */
	private final OutputStatsCallback responseInputStatsCallback;
	/** @serial */
	private final OutputStatsCallback responseCompressedStatsCallback;
	/** @serial */
	private final InputStatsCallback requestInputStatsCallback;
	/** @serial */
	private final InputStatsCallback requestCompressedStatsCallback;

	CompressingFilterStats() {
		responseInputStatsCallback = new OutputStatsCallback(StatsField.RESPONSE_INPUT_BYTES);
		responseCompressedStatsCallback = new OutputStatsCallback(StatsField.RESPONSE_COMPRESSED_BYTES);
		requestInputStatsCallback = new InputStatsCallback(StatsField.REQUEST_INPUT_BYTES);
		requestCompressedStatsCallback = new InputStatsCallback(StatsField.REQUEST_COMPRESSED_BYTES);
	}

	/**
	 * @return the number of responses which {@link CompressingFilter} has compressed.
	 */
	public int getNumResponsesCompressed() {
		return numResponsesCompressed;
	}

	void incrementNumResponsesCompressed() {
		numResponsesCompressed++;
	}

	/**
	 * @return the number of responses which {@link CompressingFilter} has processed but <em>not</em> compressed for some
	 *         reason (compression not supported by the browser, for example).
	 */
	public int getTotalResponsesNotCompressed() {
		return totalResponsesNotCompressed;
	}

	void incrementTotalResponsesNotCompressed() {
		totalResponsesNotCompressed++;
	}

	/**
	 * @deprecated use {@link #getResponseInputBytes()}
	 */
	@Deprecated
	public long getInputBytes() {
		return responseInputBytes;
	}

	/**
	 * @return total number of bytes written to the {@link CompressingFilter} in responses.
	 */
	public long getResponseInputBytes() {
		return responseInputBytes;
	}

	/**
	 * @deprecated use {@link #getResponseCompressedBytes()}
	 */
	@Deprecated
	public long getCompressedBytes() {
		return responseCompressedBytes;
	}

	/**
	 * @return total number of compressed bytes written by the {@link CompressingFilter} to the client
	 *  in responses.
	 */
	public long getResponseCompressedBytes() {
		return responseCompressedBytes;
	}

	/**
	 * @deprecated use {@link #getResponseAverageCompressionRatio()}
	 */
	@Deprecated
	public double getAverageCompressionRatio() {
		return getResponseAverageCompressionRatio();
	}

	/**
	 * @return average compression ratio (input bytes / compressed bytes) in responses,
	 *  or 0 if nothing has yet been compressed. Note that this is (typically) greater than 1, not less than 1.
	 */
	public double getResponseAverageCompressionRatio() {
		return responseCompressedBytes == 0L ? 0.0 : (double) responseInputBytes / (double) responseCompressedBytes;
	}

	/**
	 * @return the number of requests which {@link CompressingFilter} has compressed.
	 * @since 1.6
	 */
	public int getNumRequestsCompressed() {
		return numRequestsCompressed;
	}

	void incrementNumRequestsCompressed() {
		numRequestsCompressed++;
	}

	/**
	 * @return the number of requests which {@link CompressingFilter} has processed but <em>not</em> compressed for some
	 *         reason (no compression requested, for example).
	 * @since 1.6
	 */
	public int getTotalRequestsNotCompressed() {
		return totalRequestsNotCompressed;
	}

	void incrementTotalRequestsNotCompressed() {
		totalRequestsNotCompressed++;
	}

	/**
	 * @return total number of bytes written to the {@link CompressingFilter} in requests.
	 * @since 1.6
	 */
	public long getRequestInputBytes() {
		return requestInputBytes;
	}

	/**
	 * @return total number of compressed bytes written by the {@link CompressingFilter} to the client
	 *  in requests.
	 * @since 1.6
	 */
	public long getRequestCompressedBytes() {
		return requestCompressedBytes;
	}

	/**
	 * @return average compression ratio (input bytes / compressed bytes) in requests,
	 *  or 0 if nothing has yet been compressed. Note that this is (typically) greater than 1, not less than 1.
	 * @since 1.6
	 */
	public double getRequestAverageCompressionRatio() {
		return requestCompressedBytes == 0L ? 0.0 : (double) requestInputBytes / (double) requestCompressedBytes;
	}

	/**
	 * @return a summary of the stats in String form
	 */
	@Override
	public String toString() {
		return
			"CompressingFilterStats[responses compressed: " + numResponsesCompressed +
			", avg. response compression ratio: " + getResponseAverageCompressionRatio() +
			", requests compressed: " + numRequestsCompressed +
			", avg. request compression ratio: " + getRequestAverageCompressionRatio() + ']';
	}

	OutputStatsCallback getOutputStatsCallback(StatsField field) {
		switch (field) {
			case RESPONSE_INPUT_BYTES:
				return responseInputStatsCallback;
			case RESPONSE_COMPRESSED_BYTES:
				return responseCompressedStatsCallback;
			default:
				throw new IllegalArgumentException();
		}
	}

	InputStatsCallback getInputStatsCallback(StatsField field) {
		switch (field) {
			case REQUEST_INPUT_BYTES:
				return requestInputStatsCallback;
			case REQUEST_COMPRESSED_BYTES:
				return requestCompressedStatsCallback;
			default:
				throw new IllegalArgumentException();
		}
	}

	final class OutputStatsCallback implements StatsOutputStream.StatsCallback, Serializable {

		private static final long serialVersionUID = -4483355731273629325L;

		/** @serial */
		private final StatsField field;

		private OutputStatsCallback(StatsField field) {
			this.field = field;
		}

		public void bytesWritten(int numBytes) {
			assert numBytes >= 0;
			switch (field) {
				case RESPONSE_INPUT_BYTES:
					responseInputBytes += (long) numBytes;
					break;
				case RESPONSE_COMPRESSED_BYTES:
					responseCompressedBytes += (long) numBytes;
					break;
				default:
					throw new IllegalStateException();
			}
		}

		@Override
		public String toString() {
			return "OutputStatsCallback[field: " + field + ']';
		}
	}

	final class InputStatsCallback implements StatsInputStream.StatsCallback, Serializable {

		private static final long serialVersionUID = 8205059279453932247L;

		/** @serial */
		private final StatsField field;

		private InputStatsCallback(StatsField field) {
			this.field = field;
		}

		public void bytesRead(int numBytes) {
			assert numBytes >= 0;
			switch (field) {
				case REQUEST_INPUT_BYTES:
					requestInputBytes += (long) numBytes;
					break;
				case REQUEST_COMPRESSED_BYTES:
					requestCompressedBytes += (long) numBytes;
					break;
				default:
					throw new IllegalStateException();
			}
		}

		@Override
		public String toString() {
			return "InputStatsCallback[field: " + field + ']';
		}
	}

	/**
	 * <p>A simple enum used by {@link OutputStatsCallback} to select a field in this class. This is getting
	 * a little messy but somehow better than defining a bunch of inner classes?</p>
	 *
	 * @since 1.6
	 */
	enum StatsField implements Serializable {
		RESPONSE_INPUT_BYTES,
		RESPONSE_COMPRESSED_BYTES,
		REQUEST_INPUT_BYTES,
		REQUEST_COMPRESSED_BYTES
	}

}
