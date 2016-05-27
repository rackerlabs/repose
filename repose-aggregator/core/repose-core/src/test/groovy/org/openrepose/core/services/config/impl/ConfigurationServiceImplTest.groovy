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
package org.openrepose.core.services.config.impl

import org.junit.Before
import org.junit.Test
import org.mockito.InOrder
import org.openrepose.commons.config.manager.ConfigurationUpdateManager
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.config.parser.common.ConfigurationParser
import org.openrepose.commons.config.resource.ConfigurationResource
import org.openrepose.commons.config.resource.ConfigurationResourceResolver
import org.openrepose.core.services.config.ConfigurationService

import static org.mockito.Matchers.any
import static org.mockito.Mockito.*

/**
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 3/13/15
 * Time: 3:18 PM
 */
class ConfigurationServiceImplTest {
    def ConfigurationService configurationService
    def ConfigurationUpdateManager configurationUpdateManager
    private ConfigurationResourceResolver configurationResourceResolver

    @Before
    void setup() {
        configurationUpdateManager = mock(ConfigurationUpdateManager)
        configurationResourceResolver = mock(ConfigurationResourceResolver)

        configurationService = new ConfigurationServiceImpl(this.configurationUpdateManager, "butts")
        configurationService.init()
        configurationService.setResourceResolver(this.configurationResourceResolver)
    }

    @Test
    void "subscribeTo when configured to send the event initially should send the event, mark the resource read, and then add the listener"() {
        UpdateListener updateListener = mock(UpdateListener)
        ConfigurationParser configurationParser = mock(ConfigurationParser)
        ConfigurationResource configurationResource = mock(ConfigurationResource)
        when(configurationResourceResolver.resolve("butts configuration")).thenReturn(configurationResource)

        configurationService.subscribeTo("butts filter", "butts configuration", updateListener, configurationParser, true)
        InOrder inOrder = inOrder(updateListener, configurationResource, configurationUpdateManager)
        inOrder.verify(updateListener).configurationUpdated(any())
        inOrder.verify(configurationResource).updated()
        inOrder.verify(configurationUpdateManager).registerListener(updateListener, configurationResource, configurationParser, "butts filter")
    }
}
