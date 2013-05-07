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

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Tests {@link StatsOutputStream}.
 *
 * @author Sean Owen
 */
public final class StatsOutputStreamTest extends TestCase {

	private ByteArrayOutputStream baos;
	private MockStatsCallback callback;
	private OutputStream statsOut;

	@Override	
	public void setUp() throws Exception {
		super.setUp();
		baos = new ByteArrayOutputStream();
		callback = new MockStatsCallback();
		statsOut = new StatsOutputStream(baos, callback);
	}

	public void testStats() throws Exception {
		assertBytesWritten(0);
		statsOut.write(0);
		assertBytesWritten(1);
		statsOut.write(new byte[10]);
		assertBytesWritten(11);
		statsOut.write(new byte[10], 0, 5);
		assertBytesWritten(16);
		statsOut.flush();
		assertBytesWritten(16);
		statsOut.close();
		assertBytesWritten(16);
	}

	private void assertBytesWritten(int numBytes) {
		assertEquals(numBytes, callback.totalBytesWritten);
		assertEquals(numBytes, baos.size());
	}

	private static final class MockStatsCallback implements StatsOutputStream.StatsCallback {
		private int totalBytesWritten;
		public void bytesWritten(int numBytes) {
			totalBytesWritten += numBytes;
		}
	}

}
