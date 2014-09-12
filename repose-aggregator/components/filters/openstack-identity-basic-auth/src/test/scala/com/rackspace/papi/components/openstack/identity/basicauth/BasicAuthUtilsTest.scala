package com.rackspace.papi.components.openstack.identity.basicauth

import com.rackspace.papi.components.openstack.identity.basicauth.BasicAuthUtils._
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
    cases.foreach { case (input, (username, password)) =>
      it(s"decodes $input into $username and $password") {
        val inputBytes = input.getBytes()

        val (extractedUsername, extractedPassword) = extractCreds(inputBytes)

        extractedUsername shouldBe username
        extractedPassword shouldBe password
      }
    }
  }
}
