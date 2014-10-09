package com.rackspace.papi.components.datastore.impl.distributed;

import org.openrepose.commons.utils.ArrayUtilities;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.ExtendedHttpHeader;
import org.openrepose.commons.utils.http.HeaderConstant;
import org.openrepose.commons.utils.io.BufferCapacityException;
import org.openrepose.commons.utils.io.RawInputStreamReader;
import com.rackspace.papi.components.datastore.distributed.RemoteBehavior;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetSocketAddress;

public class CacheRequest {

    public static final String CACHE_URI_PATH = "/powerapi/dist-datastore/objects/";
    public static final int TWO_MEGABYTES_IN_BYTES = 2097152, EXPECTED_UUID_STRING_LENGTH = 36, DEFAULT_TTL_IN_SECONDS = 60;
    public static final HeaderConstant TTL_HEADER = ExtendedHttpHeader.X_TTL;
    public static final String TEMP_HOST_KEY = "temp-host-key";

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

    public static String urlFor(InetSocketAddress remoteEndpoint, String key) {
        return new StringBuilder("http://").append(remoteEndpoint.getAddress().getHostAddress()).append(":").append(remoteEndpoint.getPort()).append(CACHE_URI_PATH).append(key).toString();
    }

    public static String urlFor(InetSocketAddress remoteEndpoint) {
        return new StringBuilder("http://").append(remoteEndpoint.getAddress().getHostAddress()).append(":").append(remoteEndpoint.getPort()).append(CACHE_URI_PATH).toString();
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
