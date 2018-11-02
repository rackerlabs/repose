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

import org.openrepose.commons.utils.io.ObjectSerializer
import org.openrepose.core.container.config._

/**
  * This static utility object may be used to apply container configuration patches.
  */
object DeploymentConfigPatchUtil {
  /**
    * Applies a patch configuration to a copy of the base configuration.
    *
    * This function makes a deep copy up-front, then allows the other functions in this object
    * to mutate state on the copy. This was done so that the overhead of performing a deep copy
    * is only incurred once. In the future, it may be worth performing a shallow copy at each
    * function for the sake of consistency, and for some performance gain.
    *
    * @param base  the base configuration object
    * @param patch the patch configuration object, the values of which will override the base
    * @return a new configuration object with the patch applied to the base
    */
  def patch(base: DeploymentConfiguration,
            patch: DeploymentConfigurationPatch): DeploymentConfiguration = {
    val baseClone = deepCopy(base)

    Option(patch.getHttpPort).foreach(baseClone.setHttpPort)
    Option(patch.getHttpsPort).foreach(baseClone.setHttpsPort)
    Option(patch.getContentBodyReadLimit).foreach(baseClone.setContentBodyReadLimit)
    // Option(patch.getJmxResetTime).foreach(baseClone.setJmxResetTime)
    Option(patch.getIdleTimeout).foreach(baseClone.setIdleTimeout)
    Option(patch.getSoLingerTime).foreach(baseClone.setSoLingerTime)
    Option(patch.getSslConfiguration) foreach { sslConfiguration =>
      if (Option(baseClone.getSslConfiguration).isEmpty) {
        baseClone.setSslConfiguration(new SslConfiguration())
      }
      patchSslConfiguration(baseClone.getSslConfiguration, sslConfiguration)
    }
    Option(patch.getViaHeader) foreach { viaHeader =>
      if (Option(baseClone.getViaHeader).isEmpty) {
        baseClone.setViaHeader(new ViaHeader())
      }
      patchViaHeader(baseClone.getViaHeader, viaHeader)
    }

    /* INFO: These calls are commented out as patching for those nodes is not yet supported. A future update may
           add more patching support, so the code is being left in.

    Option(patch.getDeploymentDirectory) foreach { deploymentDirectory =>
      patchDeploymentDirectory(baseClone.getDeploymentDirectory, deploymentDirectory)
    }
    Option(patch.getArtifactDirectory) foreach { artifactDirectory =>
      patchArtifactDirectory(baseClone.getArtifactDirectory, artifactDirectory)
    }
    Option(patch.getLoggingConfiguration) foreach { loggingConfiguration =>
      if (Option(baseClone.getLoggingConfiguration).isEmpty) {
        baseClone.setLoggingConfiguration(new LoggingConfiguration())
      }
      patchLoggingConfiguration(baseClone.getLoggingConfiguration, loggingConfiguration)
    }
    */

    baseClone
  }

  /* INFO: These functions are commented out as patching for those nodes is not yet supported. A future update may
         add more patching support, so the code is being left in.

  /**
    * This function does not return a copy of the base configuration, but instead mutates it directly.
    *
    * @param base  the base configuration object
    * @param patch the patch configuration object, the values of which will override the base
    * @return a new configuration object with the patch applied to the base
    */
  def patchLoggingConfiguration(base: LoggingConfiguration,
                                patch: LoggingConfiguration): LoggingConfiguration = {
    Option(patch.getHref).foreach(base.setHref)
    Option(patch.getValue).foreach(base.setValue)

    base
  }

  /**
    * This function does not return a copy of the base configuration, but instead mutates it directly.
    *
    * @param base  the base configuration object
    * @param patch the patch configuration object, the values of which will override the base
    * @return a new configuration object with the patch applied to the base
    */
  def patchDeploymentDirectory(base: DeploymentDirectory,
                               patch: DeploymentDirectoryPatch): DeploymentDirectory = {
    Option(patch.getValue).foreach(base.setValue)
    Option(patch.isAutoClean).foreach(base.setAutoClean)

    base
  }

  /**
    * This function does not return a copy of the base configuration, but instead mutates it directly.
    *
    * @param base  the base configuration object
    * @param patch the patch configuration object, the values of which will override the base
    * @return a new configuration object with the patch applied to the base
    */
  def patchArtifactDirectory(base: ArtifactDirectory,
                             patch: ArtifactDirectoryPatch): ArtifactDirectory = {
    Option(patch.getValue).foreach(base.setValue)
    Option(patch.getCheckInterval).foreach(base.setCheckInterval)

    base
  }
  */

  /**
    * This function does not return a copy of the base configuration, but instead mutates it directly.
    *
    * @param base  the base configuration object
    * @param patch the patch configuration object, the values of which will override the base
    * @return the base configuration object with the patch applied
    */
  def patchSslConfiguration(base: SslConfiguration,
                            patch: SslConfigurationPatch): SslConfiguration = {
    Option(patch.getKeystoreFilename).foreach(base.setKeystoreFilename)
    Option(patch.getKeystorePassword).foreach(base.setKeystorePassword)
    Option(patch.getKeyPassword).foreach(base.setKeyPassword)
    Option(patch.getTruststoreFilename).foreach(base.setTruststoreFilename)
    Option(patch.getTruststorePassword).foreach(base.setTruststorePassword)
    Option(patch.getIncludedProtocols).foreach(base.setIncludedProtocols)
    Option(patch.getExcludedProtocols).foreach(base.setExcludedProtocols)
    Option(patch.getIncludedCiphers).foreach(base.setIncludedCiphers)
    Option(patch.getExcludedCiphers).foreach(base.setExcludedCiphers)
    Option(patch.isTlsRenegotiationAllowed).foreach(base.setTlsRenegotiationAllowed)
    Option(patch.isNeedClientAuth).foreach(base.setNeedClientAuth)

    base
  }

  /**
    * This function does not return a copy of the base configuration, but instead mutates it directly.
    *
    * @param base  the base configuration object
    * @param patch the patch configuration object, the values of which will override the base
    * @return the base configuration object with the patch applied
    */
  def patchViaHeader(base: ViaHeader,
                     patch: ViaHeaderPatch): ViaHeader = {
    Option(patch.isReposeVersion).foreach(base.setReposeVersion)
    Option(patch.getRequestPrefix).foreach(base.setRequestPrefix)
    Option(patch.getResponsePrefix).foreach(base.setResponsePrefix)

    base
  }

  private def deepCopy[T <: java.io.Serializable](obj: T): T = {
    val objectSerializer = new ObjectSerializer(getClass.getClassLoader)
    objectSerializer.readObject(objectSerializer.writeObject(obj)).asInstanceOf[T]
  }
}
