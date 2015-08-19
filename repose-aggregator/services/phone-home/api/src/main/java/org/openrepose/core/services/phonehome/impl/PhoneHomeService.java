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
package org.openrepose.core.services.phonehome.impl;

/**
 * A service which sends Repose usage data to a data collection point. This data may be used to determine common usage
 * patterns.
 * <p/>
 * Data that may be sent includes:
 * - Filter chain
 * - Services
 * - Contact information
 * <p/>
 * Note that the lifecycle of this service is self-managed, and the configuration for this service is read directly
 * from the system model configuration file. Additionally, usage data will be sent whenever an update to the system
 * model is observed, as long as the phone home service is active.
 */
public interface PhoneHomeService {

    /**
     * Reports whether or not the phone home service is enabled.
     *
     * @throws IllegalStateException if the phone home service has not yet been initialized
     */
    boolean isEnabled();

    /**
     * Sends a usage update to the data collection point, as long as the phone home service is active.
     * <p/>
     * Calling this method may cause configurations to be read.
     *
     * @throws IllegalStateException if the phone home service has not yet been initialized, or if the service is not
     *                               currently active
     */
    void sendUpdate();
}
