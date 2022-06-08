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
package org.openrepose.commons.utils.json

import org.junit.runner.RunWith
import org.openrepose.commons.utils.string.Base64Helper
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class JsonHeaderHelperTest extends FunSpec with Matchers {

  describe("anyToJsonHeader") {
    it("should properly encode a Map for transport in a HTTP header") {
      val map = Map("one" -> 1, "two" -> 2, "three" -> 3)
      val expectedEncodedJson = Base64Helper.base64EncodeUtf8("""{"one":1,"two":2,"three":3}""")

      val result = JsonHeaderHelper.anyToJsonHeader(map)

      result shouldEqual expectedEncodedJson
    }
  }

  describe("jsonHeaderToValue") {
    it("should properly decode a header value into a JSON object") {
      val encodedJson = Base64Helper.base64EncodeUtf8("""{"one":1,"two":2,"three":3}""")

      val result = JsonHeaderHelper.jsonHeaderToValue(encodedJson)

      noException should be thrownBy result.as[Map[String, Int]]
    }
  }
}
