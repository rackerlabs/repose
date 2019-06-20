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
package org.openrepose.commons.config.parser.common;

import org.apache.commons.io.IOUtils;
import org.jtwig.exceptions.ResolveValueException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.openrepose.commons.config.resource.ConfigurationResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TemplatingConfigurationParserTest {
    private static final String TEST_USER_ENV_VAR = "TEST_USER";
    private static final String TEST_USER_NAME = "World";

    private ConfigurationParser<String> baseParser = (ConfigurationParser<String>) mock(ConfigurationParser.class);
    private TemplatingConfigurationParser<String> templatingParser = new TemplatingConfigurationParser<>(baseParser);

    @Before
    public void setup() {
        baseParser = (ConfigurationParser<String>) mock(ConfigurationParser.class);
        templatingParser = new TemplatingConfigurationParser<>(baseParser);

        when(baseParser.read(any(ConfigurationResource.class)))
            .thenAnswer((Answer<String>) invocation -> {
                Object[] args = invocation.getArguments();
                ConfigurationResource resource = (ConfigurationResource) args[0];
                return IOUtils.toString(resource.newInputStream());
            });
    }

    @Test
    public void shouldReturnTheConfigurationClassOfTheBaseParser() {
        when(baseParser.configurationClass()).thenReturn(String.class);

        assertEquals(baseParser.configurationClass(), templatingParser.configurationClass());
    }

    @Test
    public void shouldTemplateEnvironmentVariableInConfigurationResource() throws IOException {
        assumeTrue(
            "The " + TEST_USER_ENV_VAR + " environment variable must be set to" + TEST_USER_NAME + " for this test",
            TEST_USER_NAME.equals(System.getenv(TEST_USER_ENV_VAR))
        );

        ConfigurationResource cfgResource = mock(ConfigurationResource.class);
        ByteArrayInputStream cfgStream = new ByteArrayInputStream(createHelloMsg("{$(" + TEST_USER_ENV_VAR + ")$}").getBytes());
        when(cfgResource.newInputStream()).thenReturn(cfgStream);

        String result = templatingParser.read(cfgResource);

        assertEquals(createHelloMsg(TEST_USER_NAME), result);
    }

    @Test
    public void shouldRemoveTemplateCommentInConfigurationResource() throws IOException {
        ConfigurationResource cfgResource = mock(ConfigurationResource.class);
        ByteArrayInputStream cfgStream = new ByteArrayInputStream(createHelloMsg("{!COMMENT!}").getBytes());
        when(cfgResource.newInputStream()).thenReturn(cfgStream);

        String result = templatingParser.read(cfgResource);

        assertEquals(createHelloMsg(""), result);
    }

    @Test(expected = ResolveValueException.class)
    public void shouldThrowExceptionWhenMissingEnvironmentVariable() throws IOException {
        assumeTrue(
            "The NOT_A_VAR environment variable must NOT be set for this test",
            System.getenv("NOT_A_VAR") == null
        );

        ConfigurationResource cfgResource = mock(ConfigurationResource.class);
        ByteArrayInputStream cfgStream = new ByteArrayInputStream(createHelloMsg("{$(NOT_A_VAR)$}").getBytes());
        when(cfgResource.newInputStream()).thenReturn(cfgStream);

        templatingParser.read(cfgResource);
    }

    private static String createHelloMsg(String name) {
        return String.format("Hello %s!", name);
    }
}
