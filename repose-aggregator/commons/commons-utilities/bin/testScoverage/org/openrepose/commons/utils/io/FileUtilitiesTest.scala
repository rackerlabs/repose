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
package org.openrepose.commons.utils.io

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class FileUtilitiesTest extends FunSpec with Matchers {

  describe("guardedAbsoluteFile") {
    it("should return a File with an absolute path given a relative child") {
      FileUtilities.guardedAbsoluteFile("/absolute/path", "relative/path").isAbsolute shouldBe true
    }

    it("should return a File with an absolute path given an absolute child") {
      FileUtilities.guardedAbsoluteFile("/absolute/path", "/absolute/path").isAbsolute shouldBe true
    }

    it("should return a File with the child's path if child path is absolute") {
      FileUtilities.guardedAbsoluteFile("/not/used", "/absolute/path").getAbsolutePath shouldEqual "/absolute/path"
    }

    it("should return a File with the parent and child's path concatenated if child path is relative") {
      FileUtilities.guardedAbsoluteFile("/absolute/path", "relative/path").getAbsolutePath shouldEqual "/absolute/path/relative/path"
    }
  }
}
