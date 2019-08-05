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
package org.openrepose.filters.openapivalidator

import com.atlassian.oai.validator.model.{ApiOperation, ApiPath, NormalisedPath, Request}
import io.swagger.v3.oas.models.{Operation, PathItem}
import org.junit.runner.RunWith
import org.openrepose.commons.utils.http.OpenStackServiceHeader.ROLES
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import org.springframework.mock.web.MockHttpServletRequest

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class RaxRolesValidatorTest
  extends FunSpec with Matchers with MockitoSugar {

  val validator = new RaxRolesValidator

  describe("validate") {
    it("should match when there's a singular user role and singular configured role; one of which matches") {
      val report = validator.validate(request("banana"), apiOperation("banana"))
      report.hasErrors shouldBe false
    }

    it("should match when there's a singular user role and multiple configured roles; one of which matches") {
      val report = validator.validate(request("banana"), apiOperation("banana", "phone"))
      report.hasErrors shouldBe false
    }

    it("should match when there are multiple user roles and singular configured role; one of which matches") {
      val report = validator.validate(request("banana", "phone"), apiOperation("banana"))
      report.hasErrors shouldBe false
    }

    it("should match when there are multiple user roles and multiple configured roles; one of which matches") {
      val report = validator.validate(request("banana", "phone"), apiOperation("banana", "time"))
      report.hasErrors shouldBe false
    }

    it("should match when there are multiple user roles and multiple configured roles; several match") {
      val report = validator.validate(request("banana", "phone"), apiOperation("banana", "phone", "time"))
      report.hasErrors shouldBe false
    }

    it("shouldn't match when there are multiple user roles and multiple configured roles; none match") {
      val report = validator.validate(request("banana", "phone"), apiOperation("adventure", "time"))
      val messages = report.getMessages
      messages.size() shouldBe 1
      messages.get(0).getKey shouldBe RaxRolesValidator.RoleValidationMessageKey
    }
  }

  def request(roles: String*): Request = {
    val request = new MockHttpServletRequest()
    roles.foreach(request.addHeader(ROLES, _))
    new HttpServletValidatorRequest(request)
  }

  def apiOperation(roles: String*): ApiOperation = {
    val operation = new Operation()
    operation.addExtension("x-rax-roles", roles.toList.asJava)
    new ApiOperation(mock[ApiPath], mock[NormalisedPath], PathItem.HttpMethod.GET, operation)
  }
}
