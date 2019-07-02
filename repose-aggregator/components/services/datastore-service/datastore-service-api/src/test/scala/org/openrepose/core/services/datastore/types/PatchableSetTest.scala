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
package org.openrepose.core.services.datastore.types

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

/**
  * Created by adrian on 1/22/16.
  */
@RunWith(classOf[JUnitRunner])
class KeystoneV2FilterCacheInvalidationTest extends FunSpec
  with Matchers {

  describe("Patch") {
    it("returns a new set with just the initialized value") {
      val set = SetPatch("butts").newFromPatch()
      set.size shouldBe 1
      set should contain("butts")
    }
  }

  describe("PatchableSet Companion object") {
    it("should return an empty set") {
      PatchableSet.empty.isEmpty shouldBe true
    }

    it("should return a set with all the provided items") {
      val set = PatchableSet(1, 2, 3, 4, 5, 1)
      set.size shouldBe 5
      set should contain allOf(1, 2, 3, 4, 5)
    }
  }

  describe("PatchableSet") {
    it("should add the value from a patch to its set") {
      val originalSet = PatchableSet.empty[Int]
      val patchedSet = originalSet.applyPatch(SetPatch(5))
      originalSet should contain(5)
      patchedSet should contain(5)
      originalSet should not be theSameInstanceAs(patchedSet)
    }
  }
}
