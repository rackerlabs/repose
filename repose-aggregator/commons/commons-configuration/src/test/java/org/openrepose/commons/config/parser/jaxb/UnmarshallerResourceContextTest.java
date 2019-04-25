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
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.utils.pooling.ResourceContext;
import org.openrepose.commons.utils.pooling.ResourceContextException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnmarshallerResourceContextTest {
    private static final String CFG_DATA = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "\n" +
            "<element>\n" +
            "    <hello>Hi there.</hello>\n" +
            "    <goodbye>See ya.</goodbye>\n" +
            "</element>\n";

    @Test
    public void shouldPerformUnmarshall() throws IOException, JAXBException, ParserConfigurationException {
        ConfigurationResource cfgResource = mock(ConfigurationResource.class);
        ByteArrayInputStream cfgStream = new ByteArrayInputStream(CFG_DATA.getBytes());
        when(cfgResource.newInputStream()).thenReturn(cfgStream);

        JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
        UnmarshallerValidator unmarshaller = new UnmarshallerValidator(jaxbContext);

        ResourceContext<UnmarshallerValidator, Object> resourceContext = new UnmarshallerResourceContext(cfgResource);

        Element element = (Element) resourceContext.perform(unmarshaller);

        assertNotNull(element);
    }

    @Test(expected = ResourceContextException.class)
    public void testPerform()
            throws IOException, JAXBException, NoSuchAlgorithmException, ParserConfigurationException {
        ConfigurationResource cfgResource = mock(ConfigurationResource.class);
        when(cfgResource.newInputStream()).thenReturn(ConfigurationResource.class.getResourceAsStream("/nonexistent_resource"));

        JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
        UnmarshallerValidator unmarshaller = new UnmarshallerValidator(jaxbContext);

        ResourceContext<UnmarshallerValidator, Object> resourceContext = new UnmarshallerResourceContext(cfgResource);

        resourceContext.perform(unmarshaller);
    }
}
