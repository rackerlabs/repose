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

import java.util.UUID;

public final class UUIDEncodingProvider implements EncodingProvider {
    private static final int QWORD_BYTE_LENGTH = 8;
    private static final int BYTE_BIT_LENGTH = 8;

    private static final int MASK = 0xFF;
    private static final int UUID_BUFFER_SIZE = 8;
    private static final int UUID_BYTE_SIZE = 16;

    private static final UUIDEncodingProvider INSTANCE = new UUIDEncodingProvider();

    private UUIDEncodingProvider() {
    }

    public static EncodingProvider getInstance() {
        return INSTANCE;
    }

    private static byte[] longToQword(long l) {
        final byte[] qWord = new byte[QWORD_BYTE_LENGTH];

        for (int p = 0, shift = 0; p < QWORD_BYTE_LENGTH; p++, shift += BYTE_BIT_LENGTH) {
            qWord[p] = (byte) ((l >> shift) & MASK);
        }

        return qWord;
    }

    private static long qwordToLong(byte[] qWord) {
        long l = 0;

        for (int p = 0, shift = 0; p < QWORD_BYTE_LENGTH; p++, shift += BYTE_BIT_LENGTH) {
            l += (long) (qWord[p] & MASK) << shift;
        }

        return l;
    }

    public static UUID bytesToUUID(byte[] uuidBytes) {
        final byte[] buffer = new byte[UUID_BUFFER_SIZE];

        System.arraycopy(uuidBytes, 0, buffer, 0, BYTE_BIT_LENGTH);

        final long msb = qwordToLong(buffer);

        System.arraycopy(uuidBytes, BYTE_BIT_LENGTH, buffer, 0, BYTE_BIT_LENGTH);

        final long lsb = qwordToLong(buffer);

        return new UUID(msb, lsb);
    }

    @Override
    public byte[] decode(String hash) {
        final UUID uuid = UUID.fromString(hash);

        final byte[] buffer = new byte[UUID_BYTE_SIZE];

        System.arraycopy(longToQword(uuid.getMostSignificantBits()), 0, buffer, 0, BYTE_BIT_LENGTH);
        System.arraycopy(longToQword(uuid.getLeastSignificantBits()), 0, buffer, BYTE_BIT_LENGTH, BYTE_BIT_LENGTH);

        return buffer;
    }

    @Override
    public String encode(byte[] hash) {
        final byte[] buffer = new byte[UUID_BUFFER_SIZE];

        System.arraycopy(hash, 0, buffer, 0, BYTE_BIT_LENGTH);

        final long msb = qwordToLong(buffer);

        System.arraycopy(hash, BYTE_BIT_LENGTH, buffer, 0, BYTE_BIT_LENGTH);

        final long lsb = qwordToLong(buffer);

        return new UUID(msb, lsb).toString();
    }

}
