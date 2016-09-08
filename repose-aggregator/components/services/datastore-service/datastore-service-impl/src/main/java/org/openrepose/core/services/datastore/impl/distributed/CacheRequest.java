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
package org.openrepose.core.services.datastore.impl.distributed;

import org.openrepose.commons.utils.ArrayUtilities;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.ExtendedHttpHeader;
import org.openrepose.commons.utils.http.HeaderConstant;
import org.openrepose.commons.utils.io.BufferCapacityException;
import org.openrepose.commons.utils.io.RawInputStreamReader;
import org.openrepose.core.services.datastore.distributed.RemoteBehavior;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetSocketAddress;

public class CacheRequest {

    public static final String CACHE_URI_PATH = "/powerapi/dist-datastore/objects/";
    public static final int TWO_MEGABYTES_IN_BYTES = 2097152, EXPECTED_UUID_STRING_LENGTH = 36, DEFAULT_TTL_IN_SECONDS = 60;
    public static final HeaderConstant TTL_HEADER = ExtendedHttpHeader.X_TTL;
    public static final String TEMP_HOST_KEY = "temp-host-key";
    private final RemoteBehavior requestedRemoteBehavior;
    private final String cacheKey, hostKey;
    private final int ttlInSeconds;
    private final byte[] payload;

    public CacheRequest(String cacheKey, String hostKey, int ttlInSeconds, byte[] payload) {
        this(cacheKey, hostKey, ttlInSeconds, payload, RemoteBehavior.ALLOW_FORWARDING);
    }

    public CacheRequest(String cacheKey, String hostKey, int ttlInSeconds, byte[] payload, RemoteBehavior requestedRemoteBehavior) {
        this.cacheKey = cacheKey;
        this.hostKey = hostKey;
        this.ttlInSeconds = ttlInSeconds;
        this.payload = ArrayUtilities.nullSafeCopy(payload);
        this.requestedRemoteBehavior = requestedRemoteBehavior;
    }

    private static String getCacheKey(HttpServletRequest request) {
        final String requestUri = request.getRequestURI();
        final String cacheKey = requestUri.substring(CACHE_URI_PATH.length()).trim();

        if (StringUtilities.isBlank(cacheKey) || cacheKey.length() != EXPECTED_UUID_STRING_LENGTH) {
            throw new MalformedCacheRequestException(MalformedCacheRequestError.CACHE_KEY_INVALID);
        }

        return cacheKey;
    }

    private static String getHostKey(HttpServletRequest request) {
        final String hostKeyHeader = request.getHeader(DatastoreHeader.HOST_KEY.toString());

        if (StringUtilities.isBlank(hostKeyHeader)) {
            throw new MalformedCacheRequestException(MalformedCacheRequestError.NO_DD_HOST_KEY);
        }

        return hostKeyHeader;
    }

    public static boolean isCacheRequestValid(HttpServletRequest request) {
        return request.getRequestURI().startsWith(CACHE_URI_PATH);
    }

    public static String urlFor(InetSocketAddress remoteEndpoint, String key, boolean useHttps) {
        String proto = "http";
        if (useHttps) {
            proto = "https";
        }
        return new StringBuilder(proto).append("://").append(remoteEndpoint.getAddress().getHostAddress()).append(":").append(remoteEndpoint.getPort()).append(CACHE_URI_PATH).append(key).toString();
    }

    public static String urlFor(InetSocketAddress remoteEndpoint, boolean useHttps) {
        String proto = "http";
        if (useHttps) {
            proto = "https";
        }
        return new StringBuilder(proto).append("://").append(remoteEndpoint.getAddress().getHostAddress()).append(":").append(remoteEndpoint.getPort()).append(CACHE_URI_PATH).toString();
    }

    public static RemoteBehavior getRequestedRemoteBehavior(HttpServletRequest request) {
        final String remoteBehaviorHeader = request.getHeader(DatastoreHeader.REMOTE_BEHAVIOR.toString());
        RemoteBehavior remoteBehavior = RemoteBehavior.ALLOW_FORWARDING;

        if (StringUtilities.isNotBlank(remoteBehaviorHeader)) {
            remoteBehavior = RemoteBehavior.valueOfOrNull(remoteBehaviorHeader.toUpperCase());

            if (remoteBehavior == null) {
                throw new MalformedCacheRequestException(MalformedCacheRequestError.UNEXPECTED_REMOTE_BEHAVIOR);
            }
        }

        return remoteBehavior;
    }

    public static CacheRequest marshallCacheRequest(HttpServletRequest request) throws MalformedCacheRequestException {
        final String cacheKey = getCacheKey(request);

        return new CacheRequest(cacheKey, getHostKey(request), -1, null, getRequestedRemoteBehavior(request));
    }

    public static CacheRequest marshallCacheRequestWithPayload(HttpServletRequest request) throws MalformedCacheRequestException {
        final String cacheKey = getCacheKey(request);
        final String hostKey = getHostKey(request);

        try {
            final String ttlHeader = request.getHeader(TTL_HEADER.toString());
            final int ttlInSeconds = StringUtilities.isBlank(ttlHeader) ? DEFAULT_TTL_IN_SECONDS : Integer.parseInt(ttlHeader);

            if (ttlInSeconds <= 0) {
                throw new MalformedCacheRequestException(MalformedCacheRequestError.TTL_HEADER_NOT_POSITIVE);
            }

            return new CacheRequest(cacheKey, hostKey, ttlInSeconds, RawInputStreamReader.instance().readFully(request.getInputStream(), TWO_MEGABYTES_IN_BYTES), getRequestedRemoteBehavior(request));
        } catch (NumberFormatException nfe) {
            throw new MalformedCacheRequestException(MalformedCacheRequestError.TTL_HEADER_NOT_POSITIVE, nfe);
        } catch (BufferCapacityException bce) {
            throw new MalformedCacheRequestException(MalformedCacheRequestError.OBJECT_TOO_LARGE, bce);
        } catch (IOException ioe) {
            throw new MalformedCacheRequestException(MalformedCacheRequestError.UNABLE_TO_READ_CONTENT, ioe);
        }
    }

    public int getTtlInSeconds() {
        return ttlInSeconds;
    }

    public boolean hasTtlSet() {
        return ttlInSeconds != -1;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public String getHostKey() {
        return hostKey;
    }

    public byte[] getPayload() {
        return (byte[]) payload.clone();
    }

    public boolean hasPayload() {
        return payload != null;
    }

    public RemoteBehavior getRequestedRemoteBehavior() {
        return requestedRemoteBehavior;
    }
}
