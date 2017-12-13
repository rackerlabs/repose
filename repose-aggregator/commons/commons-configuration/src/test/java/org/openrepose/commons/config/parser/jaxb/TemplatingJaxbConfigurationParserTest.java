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
package org.openrepose.commons.config.parser.jaxb;

import org.junit.Test;
import org.openrepose.commons.config.parser.common.ConfigurationParser;
import org.openrepose.commons.config.resource.ConfigurationResource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TemplatingJaxbConfigurationParserTest {
    private static final String TEST_USER_ENV_VAR = "TEST_USER";
    private static final String TEST_USER_NAME = "World";

    @Test
    public void shouldTemplateEnvironmentVariableInConfigurationResource() throws JAXBException, IOException {
        assumeTrue(
            "The " + TEST_USER_ENV_VAR + " environment variable must be set to" + TEST_USER_NAME + " for this test",
            TEST_USER_NAME.equals(System.getenv(TEST_USER_ENV_VAR))
        );

        final JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
        ConfigurationParser<Element> parser = new TemplatingJaxbConfigurationParser<>(Element.class, jaxbContext, null);

        ConfigurationResource cfgResource = mock(ConfigurationResource.class);
        ByteArrayInputStream cfgStream = new ByteArrayInputStream(createConfig(createHelloMsg("{$(" + TEST_USER_ENV_VAR + ")$}")).getBytes());
        when(cfgResource.newInputStream()).thenReturn(cfgStream);

        Element element = parser.read(cfgResource);

        assertNotNull(element);
        assertEquals(createHelloMsg(TEST_USER_NAME), element.hello);
    }

    @Test(expected = ClassCastException.class)
    public void shouldThrowExceptionWhenMissingEnvironmentVariable() throws JAXBException, IOException {
        assumeTrue(
            "The NOT_A_VAR environment variable must NOT be set for this test",
            System.getenv("NOT_A_VAR") == null
        );

        final JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
        ConfigurationParser<Element> parser = new TemplatingJaxbConfigurationParser<>(Element.class, jaxbContext, null);

        ConfigurationResource cfgResource = mock(ConfigurationResource.class);
        ByteArrayInputStream cfgStream = new ByteArrayInputStream(createConfig(createHelloMsg("{$(NOT_A_VAR)$}")).getBytes());
        when(cfgResource.newInputStream()).thenReturn(cfgStream);

        parser.read(cfgResource);
    }

    private static String createHelloMsg(String name) {
        return String.format("Hello %s!", name);
    }

    private static String createConfig(String helloMsg) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "\n" +
            "<element>\n" +
            "    <hello>" + helloMsg + "</hello>\n" +
            "    <goodbye>See ya.</goodbye>\n" +
            "</element>\n";
    }
}
