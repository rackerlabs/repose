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

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.openrepose.core.spring.{CoreSpringProvider, ReposeSpringProperties}
import org.scalatest.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class OpenStackIdentityV3SchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/openstack-identity-v3.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/openstack-identity-v3.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName
  val cacheTimeouts = Seq("token", "group")

  describe("schema validation") {
    it("should successfully validate with only a roles-which-bypass-project-id-check defined") {
      val config =
        """<openstack-identity-v3 xmlns="http://docs.openrepose.org/repose/openstack-identity-v3/v1.0"
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
      val config =
        """<openstack-identity-v3 xmlns="http://docs.openrepose.org/repose/openstack-identity-v3/v1.0"
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
      val config =
        """<openstack-identity-v3 xmlns="http://docs.openrepose.org/repose/openstack-identity-v3/v1.0"
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
      }.getLocalizedMessage should include("Cannot define the deprecated roles-which-bypass-project-id-check and pre-authorized-roles")
    }

    cacheTimeouts.foreach { cacheTimeout =>
      it(s"should successfully validate with a cache timeout $cacheTimeout attribute") {
        val config =
          s"""<openstack-identity-v3 xmlns="http://docs.openrepose.org/repose/openstack-identity-v3/v1.0"
             |                       connection-pool-id="identity-v3-pool">
             |    <openstack-identity-service
             |            username="admin_username"
             |            password="admin_password"
             |            uri="http://identity.example.com"/>
             |    <cache>
             |        <timeouts $cacheTimeout="300"/>
             |    </cache>
             |</openstack-identity-v3>""".stripMargin
        validator.validateConfigString(config)
      }
    }
  }

  private val coreSpringProvider = CoreSpringProvider.getInstance()
  coreSpringProvider.initializeCoreContext("/etc/repose", false)
  private val reposeVersion = coreSpringProvider.getCoreContext.getEnvironment.getProperty(
    ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.CORE.REPOSE_VERSION))
  if (reposeVersion.startsWith("9.")) {
    describe("deprecated schema validation") {
      cacheTimeouts.foreach { cacheTimeout =>
        it(s"should fail to validate when both a deprecated $cacheTimeout element and new attribute are defined") {
          val config =
            s"""<openstack-identity-v3 xmlns="http://docs.openrepose.org/repose/openstack-identity-v3/v1.0"
               |                       connection-pool-id="identity-v3-pool">
               |    <openstack-identity-service
               |            username="admin_username"
               |            password="admin_password"
               |            uri="http://identity.example.com"/>
               |    <cache>
               |        <timeouts $cacheTimeout="300">
               |            <$cacheTimeout>300</$cacheTimeout>
               |        </timeouts>
               |    </cache>
               |</openstack-identity-v3>""".stripMargin
          val
          exception = intercept[SAXParseException] {
            validator.validateConfigString(config)
          }
          exception.getLocalizedMessage should include(s"Cannot define both a deprecated $cacheTimeout element and the new $cacheTimeout attribute")
        }
      }

      cacheTimeouts.permutations.map(_.take(2)) foreach { timeouts =>
        val timeoutOne = timeouts.head
        val timeoutTwo = timeouts.tail.head
        it(s"should successfully validate with a cache timeout $timeoutOne attribute and a $timeoutTwo element") {
          val config =
            s"""<openstack-identity-v3 xmlns="http://docs.openrepose.org/repose/openstack-identity-v3/v1.0"
               |                       connection-pool-id="identity-v3-pool">
               |    <openstack-identity-service
               |            username="admin_username"
               |            password="admin_password"
               |            uri="http://identity.example.com"/>
               |    <cache>
               |        <timeouts $timeoutOne="300">
               |            <$timeoutTwo>300</$timeoutTwo>
               |        </timeouts>
               |    </cache>
               |</openstack-identity-v3>""".stripMargin
          validator.validateConfigString(config)
        }
      }
    }
  } else {
    describe("new schema validation") {
      cacheTimeouts.foreach { cacheTimeout =>
        it(s"should fail to validate when a /cache/timeouts/$cacheTimeout element is defined") {
          val config =
            s"""<openstack-identity-v3 xmlns="http://docs.openrepose.org/repose/openstack-identity-v3/v1.0"
               |                       connection-pool-id="identity-v3-pool">
               |    <openstack-identity-service
               |            username="admin_username"
               |            password="admin_password"
               |            uri="http://identity.example.com"/>
               |    <cache>
               |        <timeouts>
               |            <$cacheTimeout>300</$cacheTimeout>
               |        </timeouts>
               |    </cache>
               |</openstack-identity-v3>""".stripMargin
          val
          exception = intercept[SAXParseException] {
            validator.validateConfigString(config)
          }
          exception.getLocalizedMessage should include("not allowed")
        }
      }
    }
  }
}
