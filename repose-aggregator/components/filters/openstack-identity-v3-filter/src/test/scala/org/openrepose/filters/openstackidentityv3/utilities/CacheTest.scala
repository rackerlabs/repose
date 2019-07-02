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

import org.junit.runner.RunWith
import org.openrepose.core.services.datastore.Datastore
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class CacheTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  var mockDatastore: Datastore = _

  override def beforeEach() {
    mockDatastore = mock[Datastore]
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
}
