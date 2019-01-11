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

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.container.config.DeploymentConfiguration;

import java.util.Optional;

/**
 * The Container Configuration Service will manage the container configuration. Since the container configuration
 * is patchable, this service was made a node service so that it is able to handle said patching.
 * <p>
 * Consumers may query this service to retrieve up-to-date configured container settings.
 */
public interface ContainerConfigurationService {

    /**
     * @return the configured Via string to be added to the Request Via header.
     */
    Optional<String> getRequestVia();

    /**
     * @return the configured Via string to be added to the Response Via header.
     */
    Optional<String> getResponseVia();

    /**
     * @return if the Via string should include the Repose version.
     */
    boolean includeViaReposeVersion();

    /**
     * @return the maximum size of the request body in bytes, if configured.
     */
    Optional<Long> getContentBodyReadLimit();

    /**
     * @return the full deployment configuration patched for the node this service is running on.
     */
    DeploymentConfiguration getDeploymentConfiguration();

    /**
     * Subscribe an {@link UpdateListener} which should be notified when the container configuration is updated.
     * <p>
     * Enables users to perform some action in response to a container configuration update.
     * <p>
     * Notifies the {@link UpdateListener} of the current configuration immediately.
     *
     * @param listener the {@link UpdateListener} to be notified when the container configuration is updated.
     */
    void subscribeTo(UpdateListener<DeploymentConfiguration> listener);

    /**
     * Subscribe an {@link UpdateListener} which should be notified when the container configuration is updated.
     * <p>
     * Enables users to perform some action in response to a container configuration update.
     *
     * @param listener            the {@link UpdateListener} to be notified when the container configuration is updated.
     * @param sendNotificationNow whether or not the listener should be notified of the current configuration
     *                            immediately.
     */
    void subscribeTo(UpdateListener<DeploymentConfiguration> listener, boolean sendNotificationNow);

    /**
     * Un-subscribe an {@link UpdateListener} which should no longer be notified when the container configuration
     * is updated.
     *
     * @param listener the subscribed {@link UpdateListener} which should no longer be notified when the container
     *                 configuration is updated.
     */
    void unsubscribeFrom(UpdateListener<DeploymentConfiguration> listener);
}
