/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
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

package org.openrepose.external.pjlcompression;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link StatsInputStream}.
 *
 * @author Sean Owen
 * @since 1.6
 */
public final class StatsInputStreamTest {

    private ByteArrayInputStream bais;
    private MockStatsCallback callback;
    private InputStream statsIn;

    @Before
    public void setUp() throws Exception {
        bais = new ByteArrayInputStream(new byte[100]);
        callback = new MockStatsCallback();
        statsIn = new StatsInputStream(bais, callback);
    }

    @Test
    public void testStats() throws Exception {
        assertBytesRead(0);
        assertEquals(0, statsIn.read());
        assertBytesRead(1);
        assertEquals(10, statsIn.read(new byte[10]));
        assertBytesRead(11);
        assertEquals(5, statsIn.read(new byte[10], 0, 5));
        assertBytesRead(16);
        statsIn.close();
        assertBytesRead(16);
    }

    @Test
    public void testMarkSkipReset() throws Exception {
        assertTrue(statsIn.markSupported());
        statsIn.mark(40);
        statsIn.skip(50);
        assertBytesRead(50);
        try {
            statsIn.reset();
            // This should have throw an {@link IOException} as per {@link InputStream}
            // since we read 50 which is past the mark of 40 by 10.
            // However there is a note in {@link ByteArrayInputStream} that states:
            // Note: The readAheadLimit for this class has no meaning.
            //fail( "Did not receive the expected Exception???" );
        } catch (IOException expectedException) {
        }
        statsIn.close();
    }

    private void assertBytesRead(int numBytes) {
        assertEquals(numBytes, callback.totalBytesRead);
        assertEquals(numBytes, 100 - bais.available());
    }

    private static final class MockStatsCallback implements StatsInputStream.StatsCallback {
        private long totalBytesRead;

        public void bytesRead(long numBytes) {
            totalBytesRead += numBytes;
        }
    }
}
