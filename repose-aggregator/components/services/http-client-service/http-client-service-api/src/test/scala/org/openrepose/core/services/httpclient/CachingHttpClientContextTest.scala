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
package org.openrepose.core.services.httpclient

import org.apache.http.protocol.BasicHttpContext
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class CachingHttpClientContextTest extends FunSpec with Matchers {

  describe("adapt") {
    it("should return an enhanced context with access the underlying context") {
      val key = "test"
      val value = "testValue"
      val basicContext = new BasicHttpContext()
      basicContext.setAttribute(key, value)

      val cachingContext = CachingHttpClientContext.adapt(basicContext)

      cachingContext should not be basicContext
      cachingContext.getAttribute(key) shouldEqual value
    }

    it("should return the provided context if it is already of the correct type") {
      val cachingContext = new CachingHttpClientContext()

      val adaptedCachingContext = CachingHttpClientContext.adapt(cachingContext)

      adaptedCachingContext shouldBe cachingContext
    }
  }

  describe("create") {
    it("should create a new context") {
      val cachingContext = CachingHttpClientContext.create()

      cachingContext should not be null
    }
  }

  describe("getUseCache") {
    it("should return the value of the use cache attribute") {
      val useCache = false
      val context = CachingHttpClientContext.create()

      context.setAttribute(CachingHttpClientContext.CACHE_USE, useCache)

      context.getUseCache shouldEqual useCache
    }
  }

  describe("setUseCache") {
    it("should set the value of the use cache attribute") {
      val useCache = false
      val context = CachingHttpClientContext.create()

      context.setUseCache(useCache)

      context.getAttribute(CachingHttpClientContext.CACHE_USE) shouldEqual useCache
    }
  }

  describe("getCacheKey") {
    it("should return the value of the cache key attribute") {
      val cacheKey = "testKey"
      val context = CachingHttpClientContext.create()

      context.setAttribute(CachingHttpClientContext.CACHE_KEY, cacheKey)

      context.getCacheKey shouldEqual cacheKey
    }
  }

  describe("setCacheKey") {
    it("should set the value of the cache key attribute") {
      val cacheKey = "testKey"
      val context = CachingHttpClientContext.create()

      context.setCacheKey(cacheKey)

      context.getAttribute(CachingHttpClientContext.CACHE_KEY) shouldEqual cacheKey
    }
  }

  describe("getForceRefreshCache") {
    it("should return the value of the force refresh cache attribute") {
      val forceRefresh = true
      val context = CachingHttpClientContext.create()

      context.setAttribute(CachingHttpClientContext.CACHE_FORCE_REFRESH, forceRefresh)

      context.getForceRefreshCache shouldEqual forceRefresh
    }
  }

  describe("setForceRefreshCache") {
    it("should set the value of the force refresh cache attribute") {
      val forceRefresh = true
      val context = CachingHttpClientContext.create()

      context.setForceRefreshCache(forceRefresh)

      context.getAttribute(CachingHttpClientContext.CACHE_FORCE_REFRESH) shouldEqual forceRefresh
    }
  }
}
