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
package org.openrepose.nodeservice.test

import java.lang.Long
import java.util.Optional
import javax.inject.Named

import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.container.config.{ArtifactDirectory, DeploymentConfiguration, DeploymentDirectory}
import org.openrepose.nodeservice.containerconfiguration.ContainerConfigurationService

@Named
class FakeContainerConfigurationService extends ContainerConfigurationService {
  var via: Optional[String] = Optional.empty()
  var contentBodyReadLimit: Optional[Long] = Optional.empty()
  var deploymentConfiguration: DeploymentConfiguration = {
    val config = new DeploymentConfiguration()

    config.setDeploymentDirectory {
      val deploymentDirectory = new DeploymentDirectory()
      deploymentDirectory.setValue("/repose/deployments")
      deploymentDirectory
    }
    config.setArtifactDirectory {
      val artifactDirectory = new ArtifactDirectory()
      artifactDirectory.setValue("/repose/artifacts")
      artifactDirectory
    }

    config
  }

  override def getVia: Optional[String] = via

  override def getContentBodyReadLimit: Optional[Long] = contentBodyReadLimit

  override def getDeploymentConfiguration: DeploymentConfiguration = deploymentConfiguration

  override def subscribeTo(listener: UpdateListener[DeploymentConfiguration]): Unit = {}

  override def subscribeTo(listener: UpdateListener[DeploymentConfiguration], sendNotificationNow: Boolean): Unit = {}

  override def unsubscribeFrom(listener: UpdateListener[DeploymentConfiguration]): Unit = {}
}
