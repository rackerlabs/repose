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
package org.openrepose.filters.keystonev2

import org.junit.runner.RunWith
import org.openrepose.filters.keystonev2.KeystoneV2Common.Role
import org.openrepose.filters.keystonev2.KeystoneV2TestCommon.createValidToken
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class KeystoneV2CommonTest extends FunSpec with Matchers {

  describe("getTenantToRolesMap") {
    it("should map tenants to roles") {
      val token = createValidToken(roles = Seq(Role("role1", Some("tenant1")), Role("role2", Some("tenant1")), Role("role3", Some("tenant3"))))

      KeystoneV2Common.getTenantToRolesMap(token) should contain only("tenant1" -> Seq("role1", "role2"), "tenant3" -> Seq("role3"))
    }

    it("should include the default tenant ID") {
      val token = createValidToken(defaultTenantId = Some("defaultTenant"), roles = Seq(Role("role", Some("roleTenant"))))

      KeystoneV2Common.getTenantToRolesMap(token) should contain only("defaultTenant" -> Seq.empty[String], "roleTenant" -> Seq("role"))
    }

    it("should map domain-level roles to a constant, unique key") {
      val token = createValidToken(roles = Seq(Role("role", None)))

      KeystoneV2Common.getTenantToRolesMap(token) should contain only KeystoneV2Common.DomainRoleTenantKey -> Seq("role")
    }
  }
}


