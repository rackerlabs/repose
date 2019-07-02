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
package org.openrepose.filters.keystonev2basicauth

import org.apache.commons.codec.binary.Base64
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class BasicAuthUtilsTest extends FunSpec with Matchers with BasicAuthUtils {

  describe("decoding username and API key credentials") {
    val cases = List(
      "userName:apiKey" ->("userName", "apiKey"), // No extra colons
      "userName:::apiKey" ->("userName", "::apiKey"), // Extra leading colons
      "userName:api:::Key" ->("userName", "api:::Key"), // Extra embedded colons
      "userName:apiKey::" ->("userName", "apiKey::"), // Extra trailing colons
      "userName::a:p:i:K:e:y:" ->("userName", ":a:p:i:K:e:y:"), // Just crazy
      ":" ->("", ""), // Empty username, password
      "username:" ->("username", "") // Empty password
    )
    cases.foreach { case (decoded, (expectedUsername, expectedPassword)) =>
      it(s"decodes $decoded into $expectedUsername and $expectedPassword") {
        val authValue = new String(Base64.encodeBase64URLSafeString(decoded.getBytes))

        val (extractedUsername, extractedPassword) = extractCredentials(authValue)

        extractedUsername shouldBe expectedUsername
        extractedPassword shouldBe expectedPassword
      }
    }
  }
}
