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

package org.openrepose.filters.openstackidentityv3.config

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigValidator
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class OpenStackIdentityV3SchemaTest extends FunSpec with Matchers {
  val validator = ConfigValidator("/META-INF/schema/config/openstack-identity-v3.xsd")

  describe("schema validation") {
    it("should successfully validate the sample config") {
      validator.validateConfigFile("/META-INF/schema/examples/openstack-identity-v3.cfg.xml")
    }

    it("should successfully validate with only a roles-which-bypass-project-id-check defined") {
      val config = """<openstack-identity-v3 xmlns="http://docs.openrepose.org/repose/openstack-identity-v3/v1.0"
                     |                       connection-pool-id="identity-v3-pool">
                     |    <openstack-identity-service
                     |            username="admin_username"
                     |            password="admin_password"
                     |            uri="http://identity.example.com"/>
                     |    <roles-which-bypass-project-id-check>
                     |        <role>Role</role>
                     |    </roles-which-bypass-project-id-check>
                     |</openstack-identity-v3>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate with only a pre-authorized-roles defined") {
      val config = """<openstack-identity-v3 xmlns="http://docs.openrepose.org/repose/openstack-identity-v3/v1.0"
                     |                       connection-pool-id="identity-v3-pool">
                     |    <openstack-identity-service
                     |            username="admin_username"
                     |            password="admin_password"
                     |            uri="http://identity.example.com"/>
                     |    <pre-authorized-roles>
                     |        <role>Role</role>
                     |    </pre-authorized-roles>
                     |</openstack-identity-v3>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject config if both the deprecated roles-which-bypass-project-id-check element and the replacement pre-authorized-roles element are defined") {
      val config = """<openstack-identity-v3 xmlns="http://docs.openrepose.org/repose/openstack-identity-v3/v1.0"
                     |                       connection-pool-id="identity-v3-pool">
                     |    <openstack-identity-service
                     |            username="admin_username"
                     |            password="admin_password"
                     |            uri="http://identity.example.com"/>
                     |    <roles-which-bypass-project-id-check>
                     |        <role>Role</role>
                     |    </roles-which-bypass-project-id-check>
                     |    <pre-authorized-roles>
                     |        <role>Role</role>
                     |    </pre-authorized-roles>
                     |</openstack-identity-v3>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Cannot define the deprecated roles-which-bypass-project-id-check and pre-authorized-roles")
    }
  }
}
