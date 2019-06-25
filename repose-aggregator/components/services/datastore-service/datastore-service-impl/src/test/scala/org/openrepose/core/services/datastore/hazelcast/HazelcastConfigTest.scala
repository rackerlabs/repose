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
package org.openrepose.core.services.datastore.hazelcast

import org.junit.runner.RunWith
import org.mockito.Mockito.{mock, when}
import org.openrepose.core.services.datastore.hazelcast.HazelcastConfigTest._
import org.openrepose.core.services.datastore.hazelcast.config._
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}
import org.w3c.dom.Element

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class HazelcastConfigTest
  extends FunSpec with Matchers {

  describe("from") {
    it("should set port configuration") {
      val testConfig = simpleConfig()
      val testPort = new Port()
      testPort.setValue(5432)
      testPort.setPortCount(42)
      testPort.setAutoIncrement(true)
      testConfig.setPort(testPort)

      val hazelcastConfig = HazelcastConfig.from(testConfig)

      val networkConfig = hazelcastConfig.getNetworkConfig
      networkConfig.getPort shouldEqual testPort.getValue
      networkConfig.getPortCount shouldEqual testPort.getPortCount
      networkConfig.isPortAutoIncrement shouldEqual testPort.isAutoIncrement
    }

    it("should set join configuration for tcp/ip") {
      val testConfig = simpleConfig()
      val testJoin = new Join()
      val testTcpIp = new TcpIp()
      testTcpIp.setConnectionTimeoutSeconds(42)
      testTcpIp.getMember.add("123.123.123.123")
      testJoin.setTcpIp(testTcpIp)
      testConfig.setJoin(testJoin)

      val hazelcastConfig = HazelcastConfig.from(testConfig)

      val joinConfig = hazelcastConfig.getNetworkConfig.getJoin
      joinConfig.getMulticastConfig.isEnabled shouldBe false
      joinConfig.getAwsConfig.isEnabled shouldBe false
      joinConfig.getKubernetesConfig.isEnabled shouldBe false
      joinConfig.getTcpIpConfig.isEnabled shouldBe true
      joinConfig.getTcpIpConfig.getConnectionTimeoutSeconds shouldEqual testTcpIp.getConnectionTimeoutSeconds
      joinConfig.getTcpIpConfig.getMembers.asScala should contain theSameElementsAs testTcpIp.getMember.asScala
    }

    it("should set join configuration for multicast") {
      val testConfig = simpleConfig()
      val testJoin = new Join()
      val testMulticast = new Multicast()
      testMulticast.setMulticastGroup("112.1.1.2")
      testMulticast.setMulticastPort(9876)
      testMulticast.setMulticastTimeoutSeconds(42)
      testMulticast.setMulticastTimeToLive(43)
      testJoin.setMulticast(testMulticast)
      testConfig.setJoin(testJoin)

      val hazelcastConfig = HazelcastConfig.from(testConfig)

      val joinConfig = hazelcastConfig.getNetworkConfig.getJoin
      joinConfig.getTcpIpConfig.isEnabled shouldBe false
      joinConfig.getAwsConfig.isEnabled shouldBe false
      joinConfig.getKubernetesConfig.isEnabled shouldBe false
      joinConfig.getMulticastConfig.isEnabled shouldBe true
      joinConfig.getMulticastConfig.getMulticastGroup shouldEqual testMulticast.getMulticastGroup
      joinConfig.getMulticastConfig.getMulticastPort shouldEqual testMulticast.getMulticastPort
      joinConfig.getMulticastConfig.getMulticastTimeoutSeconds shouldEqual testMulticast.getMulticastTimeoutSeconds
      joinConfig.getMulticastConfig.getMulticastTimeToLive shouldEqual testMulticast.getMulticastTimeToLive
    }

    it("should set join configuration for aws") {
      val testConfig = simpleConfig()
      val testJoin = new Join()
      val testAws = new AliasedDiscoveryStrategy()
      testAws.setConnectionTimeoutSeconds(42)
      testAws.getAny.add(element("access-key", "access-key"))
      testAws.getAny.add(element("secret-key", "secret-key"))
      testAws.getAny.add(element("tag-key", "foo"))
      testAws.getAny.add(element("tag-value", " bar "))
      testJoin.setAws(testAws)
      testConfig.setJoin(testJoin)

      val hazelcastConfig = HazelcastConfig.from(testConfig)

      val joinConfig = hazelcastConfig.getNetworkConfig.getJoin
      joinConfig.getMulticastConfig.isEnabled shouldBe false
      joinConfig.getTcpIpConfig.isEnabled shouldBe false
      joinConfig.getKubernetesConfig.isEnabled shouldBe false
      joinConfig.getAwsConfig.isEnabled shouldBe true
      joinConfig.getAwsConfig.getProperty("connection-timeout-seconds") shouldEqual String.valueOf(testAws.getConnectionTimeoutSeconds)
      joinConfig.getAwsConfig.getProperty("access-key") shouldEqual "access-key"
      joinConfig.getAwsConfig.getProperty("secret-key") shouldEqual "secret-key"
      joinConfig.getAwsConfig.getProperty("tag-key") shouldEqual "foo"
      joinConfig.getAwsConfig.getProperty("tag-value") shouldEqual "bar"
    }

    it("should set join configuration for kubernetes") {
      val testConfig = simpleConfig()
      val testJoin = new Join()
      val testKubernetes = new AliasedDiscoveryStrategy()
      testKubernetes.setConnectionTimeoutSeconds(42)
      testKubernetes.getAny.add(element("namespace", "namespace"))
      testKubernetes.getAny.add(element("service-name", "serviceName"))
      testKubernetes.getAny.add(element("service-label-name", " serviceLabelName "))
      testKubernetes.getAny.add(element("service-label-value", " serviceLabelValue "))
      testJoin.setKubernetes(testKubernetes)
      testConfig.setJoin(testJoin)

      val hazelcastConfig = HazelcastConfig.from(testConfig)

      val joinConfig = hazelcastConfig.getNetworkConfig.getJoin
      joinConfig.getMulticastConfig.isEnabled shouldBe false
      joinConfig.getTcpIpConfig.isEnabled shouldBe false
      joinConfig.getAwsConfig.isEnabled shouldBe false
      joinConfig.getKubernetesConfig.isEnabled shouldBe true
      joinConfig.getKubernetesConfig.getProperty("connection-timeout-seconds") shouldEqual String.valueOf(testKubernetes.getConnectionTimeoutSeconds)
      joinConfig.getKubernetesConfig.getProperty("namespace") shouldEqual "namespace"
      joinConfig.getKubernetesConfig.getProperty("service-name") shouldEqual "serviceName"
      joinConfig.getKubernetesConfig.getProperty("service-label-name") shouldEqual "serviceLabelName"
      joinConfig.getKubernetesConfig.getProperty("service-label-value") shouldEqual "serviceLabelValue"
    }
  }
}

object HazelcastConfigTest {
  def simpleConfig(): SimplifiedConfig = {
    val testConfig = new SimplifiedConfig()
    val testPort = new Port()
    val testJoin = new Join()
    val testTcpIp = new TcpIp()

    testTcpIp.getMember.add("127.0.0.1")
    testJoin.setTcpIp(testTcpIp)
    testPort.setValue(5701)
    testConfig.setPort(testPort)
    testConfig.setJoin(testJoin)

    testConfig
  }

  def element(localName: String, textContent: String): Element = {
    val element = mock(classOf[Element])
    when(element.getLocalName).thenReturn(localName)
    when(element.getTextContent).thenReturn(textContent)
    element
  }
}
