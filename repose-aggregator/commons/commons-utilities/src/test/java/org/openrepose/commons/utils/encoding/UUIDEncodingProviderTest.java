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
package org.openrepose.commons.utils.encoding;

import org.junit.Test;

import java.security.MessageDigest;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class UUIDEncodingProviderTest {

    @Test
    public void shouldEncodeMD5HashValues() throws Exception {
        final String expectedUuidValue = "cecda330-5a61-26cd-1a71-d5fe34a8e302";
        final byte[] hashBytes = MessageDigest.getInstance("MD5").digest("object-key".getBytes());

        assertEquals("UUID generated must match expected value", expectedUuidValue, UUIDEncodingProvider.getInstance().encode(hashBytes));
    }

    @Test
    public void shouldConvertWellFormedUUIDStrings() {
        final byte[] expectedBytes = new byte[16];

        for (int i = 0; i < expectedBytes.length; i++) {
            expectedBytes[i] = 1;
        }

        final UUID uuid = UUID.fromString(UUIDEncodingProvider.getInstance().encode(expectedBytes));
        final byte[] actualBytes = UUIDEncodingProvider.getInstance().decode(uuid.toString());

        assertThat(actualBytes, equalTo(expectedBytes));
    }
}
