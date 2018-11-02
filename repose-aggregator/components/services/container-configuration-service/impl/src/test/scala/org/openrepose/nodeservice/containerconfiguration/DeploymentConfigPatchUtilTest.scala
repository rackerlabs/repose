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
package org.openrepose.nodeservice.containerconfiguration

import org.junit.runner.RunWith
import org.openrepose.core.container.config._
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class DeploymentConfigPatchUtilTest extends FunSpec with Matchers {

  describe("patch") {
    val baseConfig = new DeploymentConfiguration()
    baseConfig.setHttpPort(80)
    baseConfig.setHttpsPort(443)
    baseConfig.setIdleTimeout(1000L)
    baseConfig.setJmxResetTime(1000)
    baseConfig.setSoLingerTime(1000)
    baseConfig.setContentBodyReadLimit(1000L)
    val viaHeader = new ViaHeader()
    viaHeader.setRequestPrefix("via")
    baseConfig.setViaHeader(viaHeader)

    val patchConfig = new DeploymentConfigurationPatch()
    patchConfig.setHttpPort(8080)
    patchConfig.setHttpsPort(44300)
    patchConfig.setIdleTimeout(2000L)
    // INFO: This assertion is commented out as patching for this field not yet supported. A future update may
    //       add more patching support, so the code is being left in.
    // patchConfig.setJmxResetTime(3000)
    patchConfig.setSoLingerTime(4000)
    patchConfig.setContentBodyReadLimit(5000L)
    val viaHeaderPatch = new ViaHeaderPatch()
    viaHeaderPatch.setRequestPrefix("patched-via")
    patchConfig.setViaHeader(viaHeaderPatch)

    it("should return a new instance of the configuration, not mutate the given instance") {
      val patchedConfig = DeploymentConfigPatchUtil.patch(baseConfig, patchConfig)

      patchedConfig should not be theSameInstanceAs(baseConfig)
    }

    it("should override all patched configuration") {
      val patchedConfig = DeploymentConfigPatchUtil.patch(baseConfig, patchConfig)

      patchedConfig.getHttpPort shouldEqual 8080
      patchedConfig.getHttpsPort shouldEqual 44300
      patchedConfig.getIdleTimeout shouldEqual 2000L
      // INFO: This assertion is commented out as patching for this field not yet supported. A future update may
      //       add more patching support, so the code is being left in.
      // patchedConfig.getJmxResetTime shouldEqual 3000
      patchedConfig.getSoLingerTime shouldEqual 4000
      patchedConfig.getContentBodyReadLimit shouldEqual 5000L
      patchedConfig.getViaHeader.getRequestPrefix shouldEqual "patched-via"
    }
  }

  /* INFO: These tests are commented out as patching for those nodes is not yet supported. A future update may
           add more patching support, so the tests are being left in.

  describe("patchLoggingConfiguration") {
    val baseConfig = new LoggingConfiguration()
    baseConfig.setValue("base")
    baseConfig.setHref("baseHref")

    val patchConfig = new LoggingConfiguration()
    patchConfig.setValue("patch")
    patchConfig.setHref("patchHref")

    it("should mutate and return the same instance of the configuration") {
      val patchedConfig = DeploymentConfigPatchUtil.patchLoggingConfiguration(baseConfig, patchConfig)

      patchedConfig should be theSameInstanceAs baseConfig
    }

    it("should override all patched configuration") {
      DeploymentConfigPatchUtil.patchLoggingConfiguration(baseConfig, patchConfig)

      baseConfig.getValue shouldEqual "patch"
      baseConfig.getHref shouldEqual "patchHref"
    }
  }

  describe("patchDeploymentDirectory") {
    val baseConfig = new DeploymentDirectory()
    baseConfig.setValue("base")
    baseConfig.setAutoClean(true)

    val patchConfig = new DeploymentDirectoryPatch()
    patchConfig.setValue("patch")
    patchConfig.setAutoClean(false)

    it("should mutate and return the same instance of the configuration") {
      val patchedConfig = DeploymentConfigPatchUtil.patchDeploymentDirectory(baseConfig, patchConfig)

      patchedConfig should be theSameInstanceAs baseConfig
    }

    it("should override all patched configuration") {
      DeploymentConfigPatchUtil.patchDeploymentDirectory(baseConfig, patchConfig)

      baseConfig.getValue shouldEqual "patch"
      baseConfig.isAutoClean shouldBe false
    }
  }

  describe("patchArtifactDirectory") {
    val baseConfig = new ArtifactDirectory()
    baseConfig.setValue("base")
    baseConfig.setCheckInterval(1000)

    val patchConfig = new ArtifactDirectoryPatch()
    patchConfig.setValue("patch")
    patchConfig.setCheckInterval(2000)

    it("should mutate and return the same instance of the configuration") {
      val patchedConfig = DeploymentConfigPatchUtil.patchArtifactDirectory(baseConfig, patchConfig)

      patchedConfig should be theSameInstanceAs baseConfig
    }

    it("should override all patched configuration") {
      DeploymentConfigPatchUtil.patchArtifactDirectory(baseConfig, patchConfig)

      baseConfig.getValue shouldEqual "patch"
      baseConfig.getCheckInterval shouldEqual 2000
    }
  }
  */

  describe("patchSslConfiguration") {
    val baseConfig = new SslConfiguration()
    baseConfig.setKeystoreFilename("keystore.jks")
    baseConfig.setKeystorePassword("keystore-password")
    baseConfig.setKeyPassword("key-password")
    baseConfig.setTruststoreFilename("truststore.jks")
    baseConfig.setTruststorePassword("truststore-password")
    baseConfig.setTlsRenegotiationAllowed(true)
    baseConfig.setNeedClientAuth(true)

    val patchConfig = new SslConfigurationPatch()
    patchConfig.setKeystoreFilename("patch-keystore.jks")
    patchConfig.setKeystorePassword("patch-keystore-password")
    patchConfig.setKeyPassword("patch-key-password")
    patchConfig.setTruststoreFilename("patch-truststore.jks")
    patchConfig.setTruststorePassword("patch-truststore-password")
    patchConfig.setTlsRenegotiationAllowed(false)
    patchConfig.setNeedClientAuth(false)
    patchConfig.setIncludedProtocols {
      val config = new SslProtocolConfiguration()
      config.getProtocol.add("PATCH-PROTO-INCL")
      config
    }
    patchConfig.setExcludedProtocols {
      val config = new SslProtocolConfiguration()
      config.getProtocol.add("PATCH-PROTO-EXCL")
      config
    }
    patchConfig.setIncludedCiphers {
      val config = new SslCipherConfiguration()
      config.getCipher.add("PATCH-CIPHER-INCL")
      config
    }
    patchConfig.setExcludedCiphers {
      val config = new SslCipherConfiguration()
      config.getCipher.add("PATCH-CIPHER-EXCL")
      config
    }

    it("should mutate and return the same instance of the configuration") {
      val patchedConfig = DeploymentConfigPatchUtil.patchSslConfiguration(baseConfig, patchConfig)

      patchedConfig should be theSameInstanceAs baseConfig
    }

    it("should override all patched configuration") {
      DeploymentConfigPatchUtil.patchSslConfiguration(baseConfig, patchConfig)

      baseConfig.getKeystoreFilename shouldEqual "patch-keystore.jks"
      baseConfig.getKeystorePassword shouldEqual "patch-keystore-password"
      baseConfig.getKeyPassword shouldEqual "patch-key-password"
      baseConfig.getTruststoreFilename shouldEqual "patch-truststore.jks"
      baseConfig.getTruststorePassword shouldEqual "patch-truststore-password"
      baseConfig.isTlsRenegotiationAllowed shouldBe false
      baseConfig.isNeedClientAuth shouldBe false
      baseConfig.getIncludedProtocols.getProtocol should contain only "PATCH-PROTO-INCL"
      baseConfig.getExcludedProtocols.getProtocol should contain only "PATCH-PROTO-EXCL"
      baseConfig.getIncludedCiphers.getCipher should contain only "PATCH-CIPHER-INCL"
      baseConfig.getExcludedCiphers.getCipher should contain only "PATCH-CIPHER-EXCL"
    }

    it("should overwrite existing collection when patching") {
      val localBaseConfig = new SslConfiguration()
      localBaseConfig.setIncludedProtocols {
        val config = new SslProtocolConfiguration()
        config.getProtocol.add("PROTO-INCL")
        config
      }
      localBaseConfig.setExcludedProtocols {
        val config = new SslProtocolConfiguration()
        config.getProtocol.add("PROTO-EXCL")
        config
      }
      localBaseConfig.setIncludedCiphers {
        val config = new SslCipherConfiguration()
        config.getCipher.add("CIPHER-INCL")
        config
      }
      localBaseConfig.setExcludedCiphers {
        val config = new SslCipherConfiguration()
        config.getCipher.add("CIPHER-EXCL")
        config
      }

      DeploymentConfigPatchUtil.patchSslConfiguration(localBaseConfig, patchConfig)

      localBaseConfig.getIncludedProtocols.getProtocol should contain only "PATCH-PROTO-INCL"
      localBaseConfig.getExcludedProtocols.getProtocol should contain only "PATCH-PROTO-EXCL"
      localBaseConfig.getIncludedCiphers.getCipher should contain only "PATCH-CIPHER-INCL"
      localBaseConfig.getExcludedCiphers.getCipher should contain only "PATCH-CIPHER-EXCL"
    }
  }
}
