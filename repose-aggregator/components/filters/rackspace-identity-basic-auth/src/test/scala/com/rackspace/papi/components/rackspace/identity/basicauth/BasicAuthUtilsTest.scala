package com.rackspace.papi.components.rackspace.identity.basicauth

import com.rackspace.papi.components.rackspace.identity.basicauth.BasicAuthUtils._
import org.apache.commons.codec.binary.Base64
import org.scalatest.{FunSpec, Matchers}

class BasicAuthUtilsTest extends FunSpec with Matchers {

  describe("decoding username and API key credentials") {
    val cases = List(
      "userName:apiKey" ->("userName", "apiKey"), // No extra colons
      "userName:api:::Key" ->("userName", "api:::Key"), // Extra leading colons
      "userName:::apiKey" ->("userName", "::apiKey"), // Extra embedded colons
      "userName:apiKey::" ->("userName", "apiKey::"), // Extra trailing colons
      "userName::a:p:i:K:e:y:" ->("userName", ":a:p:i:K:e:y:")
    )
    cases.foreach { case (decoded, (expectedUsername, expectedPassword)) =>
      it(s"decodes $decoded into $expectedUsername and $expectedPassword") {
        val authValue = new String(Base64.encodeBase64URLSafeString(decoded.getBytes()))

        val (extractedUsername, extractedPassword) = extractCreds(authValue)

        extractedUsername shouldBe expectedUsername
        extractedPassword shouldBe expectedPassword
      }
    }
  }
}
