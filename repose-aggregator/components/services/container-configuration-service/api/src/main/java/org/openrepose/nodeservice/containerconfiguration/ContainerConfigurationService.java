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
package org.openrepose.nodeservice.containerconfiguration;

import org.openrepose.core.container.config.DeploymentConfiguration;

import java.util.Optional;

/**
 * The Container Configuration Service will manage the container configuration. Since the container configuration
 * is patchable, this service was made a node service so that it is able to handle said patching.
 * <p>
 * Consumers may query this service to retrieve up-to-date configured container settings for their cluster.
 */
public interface ContainerConfigurationService {

    /**
     * @return the configured Via string to be added to the Via header.
     */
    Optional<String> getVia();

    /**
     * @return the maximum size of the request body in bytes, if configured.
     */
    Optional<Long> getContentBodyReadLimit();

    /**
     * @return the full deployment configuration patched for the node this service is running on.
     */
    DeploymentConfiguration getDeploymentConfiguration();

    // TODO: Should the service allow users to register callbacks? By doing so, the service could fully replace
    //       container configuration listeners for any component operating at the node level. Without such a mechanism,
    //       components like the RequestHeaderService must listen to the container configuration to respond to changes.
}
