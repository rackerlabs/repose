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
package org.openrepose.filters.openstackidentityv3.utilities

import java.util.concurrent.TimeUnit

import org.junit.runner.RunWith
import org.mockito.Mockito.{verify, when}
import org.openrepose.core.services.datastore.Datastore
import org.openrepose.core.services.datastore.types.StringValue
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class CacheTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  var mockDatastore: Datastore = _
  var cache: Cache = _

  override def beforeEach() {
    mockDatastore = mock[Datastore]
    cache = new Cache(mockDatastore)
  }

  describe("getTokenKey") {
    it("should return a unique cache key") {
      val token = "some-token"

      Cache.getTokenKey(token) shouldEqual s"${Cache.TokenKeyPrefix}$token"
    }
  }

  describe("getGroupsKey") {
    it("should return a unique cache key") {
      val token = "some-token"

      Cache.getGroupsKey(token) shouldEqual s"${Cache.GroupsKeyPrefix}$token"
    }
  }

  describe("getUserIdKey") {
    it("should return a unique cache key") {
      val userId = "some-user"

      Cache.getUserIdKey(userId) shouldEqual s"${Cache.UserIdKeyPrefix}$userId"
    }
  }

  describe("offsetTtl") {
    it("should return the configured ttl is offset is 0") {
      Cache.offsetTtl(1000, 0) shouldBe 1000
    }

    it("should return 0 if the configured ttl is 0") {
      Cache.offsetTtl(0, 1000) shouldBe 0
    }

    it("should return a random int between configured ttl +/- offset") {
      val firstCall = Cache.offsetTtl(1000, 100)
      val secondCall = Cache.offsetTtl(1000, 100)
      val thirdCall = Cache.offsetTtl(1000, 100)

      firstCall shouldBe 1000 +- 100
      secondCall shouldBe 1000 +- 100
      thirdCall shouldBe 1000 +- 100
      firstCall should (not be secondCall or not be thirdCall)
    }
  }

  describe("safeLongToInt") {
    it("should convert a Long greater than the max Int value to the max Int value") {
      Cache.safeLongToInt(3000000000L) shouldEqual Int.MaxValue
    }

    it("should convert a Long less than the min Int value to the min Int value") {
      Cache.safeLongToInt(-3000000000L) shouldEqual Int.MinValue
    }
  }

  describe("safeGet") {
    it("should return None if the key does not exist in the cache") {
      val key = "some-key"

      cache.safeGet(key, classOf[String]) shouldBe None
    }

    it("should return None if the value associated with a key is not of the correct type") {
      val key = "some-key"
      when(mockDatastore.get(key)).thenReturn(10, Nil: _*)

      cache.safeGet(key, classOf[String]) shouldBe None
    }

    it("should return Some(value) if the key exists and the associated value is of the correct type") {
      val key = "some-key"
      val value = "some-value"
      when(mockDatastore.get(key)).thenReturn(value, Nil: _*)

      cache.safeGet(key, classOf[String]) shouldEqual Some(value)
    }
  }

  describe("Datastore methods") {
    it("should delegate get to the wrapped Datastore") {
      val key = "some-key"

      cache.get(key)

      verify(mockDatastore).get(key)
    }

    it("should delegate removeAll to the wrapped Datastore") {
      cache.removeAll()

      verify(mockDatastore).removeAll()
    }

    it("should delegate getName to the wrapped Datastore") {
      cache.getName

      verify(mockDatastore).getName
    }

    it("should delegate put to the wrapped Datastore") {
      val key = "some-key"
      val value = "some-value"

      cache.put(key, value)
      cache.put(key, value, 10, TimeUnit.SECONDS)

      verify(mockDatastore).put(key, value)
      verify(mockDatastore).put(key, value, 10, TimeUnit.SECONDS)
    }

    it("should delegate remove to the wrapped Datastore") {
      val key = "some-key"

      cache.remove(key)

      verify(mockDatastore).remove(key)
    }

    it("should delegate patch to the wrapped Datastore") {
      val key = "some-key"
      val patch = new StringValue.Patch("some-value")

      cache.patch(key, patch)
      cache.patch(key, patch, 10, TimeUnit.SECONDS)

      verify(mockDatastore).patch(key, patch)
      verify(mockDatastore).patch(key, patch, 10, TimeUnit.SECONDS)
    }
  }
}
