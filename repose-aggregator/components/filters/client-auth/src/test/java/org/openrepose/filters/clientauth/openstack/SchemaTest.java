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
package org.openrepose.filters.clientauth.openstack;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.filters.clientauth.config.ClientAuthConfig;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import static org.junit.Assert.assertNotNull;

@RunWith(Enclosed.class)
public class SchemaTest {

    public static final SchemaFactory SCHEMA_FACTORY = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");

    public static class WhenValidating {

        private JAXBContext jaxbContext;
        private Unmarshaller jaxbUnmarshaller;

        @Before
        public void standUp() throws Exception {
            jaxbContext = JAXBContext.newInstance(
                    org.openrepose.filters.clientauth.config.ObjectFactory.class,
                    org.openrepose.filters.clientauth.basic.config.ObjectFactory.class,
                    org.openrepose.filters.clientauth.openstack.config.ObjectFactory.class);

            jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            jaxbUnmarshaller.setSchema(SCHEMA_FACTORY.newSchema(
                    new StreamSource[]{
                            new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/config/openstack-ids-auth/openstack-ids-auth.xsd")),
                            new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/config/http-basic/http-basic.xsd")),
                            new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/config/client-auth-n-configuration.xsd"))
                    }));
        }

        @Test
        public void shouldValidateAgainstStaticExample() throws Exception {
            final StreamSource sampleSource = new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/examples/openstack/client-auth-n.cfg.xml"));

            assertNotNull("Expected element should not be null", jaxbUnmarshaller.unmarshal(sampleSource, ClientAuthConfig.class).getValue().getOpenstackAuth());
        }
    }
}
