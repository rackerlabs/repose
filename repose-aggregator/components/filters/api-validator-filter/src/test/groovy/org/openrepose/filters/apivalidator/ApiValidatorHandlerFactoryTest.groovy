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
package org.openrepose.filters.apivalidator

import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.openrepose.commons.config.parser.generic.GenericResourceConfigurationParser
import org.openrepose.commons.config.resource.ConfigurationResource
import org.openrepose.components.apivalidator.servlet.config.DelegatingType
import org.openrepose.components.apivalidator.servlet.config.ValidatorConfiguration
import org.openrepose.components.apivalidator.servlet.config.ValidatorItem
import org.openrepose.core.services.config.ConfigurationService

import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertThat
import static org.mockito.Matchers.any
import static org.mockito.Matchers.eq
import static org.mockito.Mockito.*

@RunWith(Enclosed.class)
public class ApiValidatorHandlerFactoryTest {

    public static class WhenCreatingHandlers {
        private final String wadl = "default.wadl"
        private final String dot = "default.dot"
        private final String role = "testRole"
        private final String defaultRole = "defaultRole"
        private ConfigurationService configService
        private ApiValidatorHandlerFactory instance
        private List<String> roles
        private ValidatorConfiguration config

        @Before
        public void setup() throws Exception {
            config = new ValidatorConfiguration()
            ValidatorItem item = new ValidatorItem()
            item.setWadl(wadl)
            List<String> role1 = item.getRole()
            role1.add(role)
            config.getValidator().add(item)

            ValidatorItem defaultItem = new ValidatorItem()
            defaultItem.setWadl(wadl)
            List<String> role2 = defaultItem.getRole()
            role2.add(defaultRole)

            defaultItem.setDefault(Boolean.TRUE)
            defaultItem.setDotOutput(dot)
            config.getValidator().add(defaultItem)

            configService = mock(ConfigurationService.class)
            URL resource = this.getClass().getClassLoader().getResource("")
            instance = new ApiValidatorHandlerFactory(configService, resource.getPath(), "", Optional.empty())

            instance.configurationUpdated(config)

            roles = [role]

        }

        @Test
        public void shouldBuildValidatorListAndSubscribeToWadl() {
            ApiValidatorHandler handler = instance.buildHandler()
            assertNotNull("Should build handler", handler)

            List<ValidatorInfo> validatorsForRole = handler.getValidatorsForRoles(roles)
            assertNotNull(validatorsForRole)

            for (ValidatorInfo validatorForRole : validatorsForRole) {
                assertEquals("Should get validator for role", role, validatorForRole.getRoles().get(0))
            }
            verify(configService, times(2)).subscribeTo(eq("api-validator"), eq(instance.getWadlPath(wadl)),
                    any(ApiValidatorHandlerFactory.ApiValidatorWadlListener.class),
                    any(GenericResourceConfigurationParser.class))
        }

        @Test
        public void shouldSetDefaultValidator() {
            ApiValidatorHandler handler = instance.buildHandler()
            assertNotNull("Should build handler", handler)
            List<ValidatorInfo> validatorsForRole = handler.getValidatorsForRoles(new ArrayList<String>())
            assertNotNull(validatorsForRole)
            assertEquals("Should get validator for default role", defaultRole, validatorsForRole.get(0).getRoles().get(0))
        }

        @Test
        public void shouldPassDelegatingModeToHandler() throws Exception {
            DelegatingType delegating = new DelegatingType()
            delegating.setQuality(0.1)
            config.setDelegating(delegating)
            instance.configurationUpdated(config)
            ApiValidatorHandler handler = instance.buildHandler()
            assertThat(handler.delegatingMode, equalTo(true))
        }
    }

    public static class WhenWadlChanges {
        private final String wadl = "default.wadl"
        private final String wadl1 = "default1.wadl"
        private final String role1 = "role1"
        private final String wadl2 = "default2.wadl"
        private final String role2 = "role2"
        private ConfigurationService configService
        private ApiValidatorHandlerFactory instance
        private ValidatorInfo info1
        private ValidatorInfo info2

        @Before
        public void setup() {
            configService = mock(ConfigurationService.class)
            URL resource = this.getClass().getClassLoader().getResource(wadl)
            instance = new ApiValidatorHandlerFactory(configService, resource.getPath(), "", null)

            List<ValidatorInfo> validators = new ArrayList<ValidatorInfo>()
            info1 = mock(ValidatorInfo.class)
            when(info1.getUri()).thenReturn(instance.getWadlPath(wadl1))
            when(info1.getRoles()).thenReturn(Arrays.asList(role1))
            validators.add(info1)

            info2 = mock(ValidatorInfo.class)
            when(info2.getUri()).thenReturn(instance.getWadlPath(wadl2))
            when(info2.getRoles()).thenReturn(Arrays.asList(role2))
            validators.add(info2)

            instance.setValidators(validators)
        }

        @Test
        public void shouldClearMatchedValidator() throws MalformedURLException {
            String wadl2Path = new URL(instance.getWadlPath(wadl2)).toString()
            ConfigurationResource resource = mock(ConfigurationResource.class)
            when(resource.name()).thenReturn(wadl2Path)

            instance.getWadlListener().configurationUpdated(resource)

            verify(info1, times(0)).reinitValidator()
            verify(info2).reinitValidator()
        }

        @Test
        public void shouldClearAllValidatorsIfNoMatch() throws MalformedURLException {
            String wadl2Path = new URL(instance.getWadlPath("doesn'texist.wadl")).toString()
            ConfigurationResource resource = mock(ConfigurationResource.class)
            when(resource.name()).thenReturn(wadl2Path)

            instance.getWadlListener().configurationUpdated(resource)

            verify(info1).reinitValidator()
            verify(info2).reinitValidator()
        }
    }
}
