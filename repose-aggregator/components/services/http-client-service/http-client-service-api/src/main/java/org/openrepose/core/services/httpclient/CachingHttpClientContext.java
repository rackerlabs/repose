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
package org.openrepose.core.services.httpclient;

import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;

/**
 * Adaptor class that provides convenient, type-safe getters and setters
 * for cache control settings.
 */
public class CachingHttpClientContext extends HttpClientContext {

    /**
     * Designates whether or not the cache should be used.
     * <p>
     * If used, the {@link org.apache.http.client.HttpClient} will attempt
     * to satisfy the HTTP request associated with this context with a
     * previously cached interaction.
     */
    public static final String CACHE_USE = "repose.cache.use";

    /**
     * Identifies what cache data may be used to satisfy the HTTP request
     * if the cache should be used.
     * <p>
     * This attribute must be set
     */
    public static final String CACHE_KEY = "repose.cache.key";

    /**
     * Designates whether or not to overwrite the cache data for the
     * given cache key if cache data was not used to satisfy the request.
     */
    public static final String CACHE_FORCE_REFRESH = "repose.cache.refresh";

    public static CachingHttpClientContext adapt(final HttpContext context) {
        if (context instanceof HttpClientContext) {
            return (CachingHttpClientContext) context;
        } else {
            return new CachingHttpClientContext(context);
        }
    }

    public static CachingHttpClientContext create() {
        return new CachingHttpClientContext();
    }

    public CachingHttpClientContext(final HttpContext context) {
        super(context);
    }

    public CachingHttpClientContext() {
        super();
    }

    public Boolean getUseCache() {
        return getAttribute(CACHE_USE, Boolean.class);
    }

    public CachingHttpClientContext setUseCache(Boolean useCache) {
        setAttribute(CACHE_USE, useCache);
        return this;
    }

    public String getCacheKey() {
        return getAttribute(CACHE_KEY, String.class);
    }

    public CachingHttpClientContext setCacheKey(String cacheKey) {
        setAttribute(CACHE_KEY, cacheKey);
        return this;
    }

    public Boolean getForceRefreshCache() {
        return getAttribute(CACHE_FORCE_REFRESH, Boolean.class);
    }

    public CachingHttpClientContext setForceRefreshCache(Boolean forceRefreshCache) {
        setAttribute(CACHE_FORCE_REFRESH, forceRefreshCache);
        return this;
    }
}
