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

import org.apache.commons.io.IOUtils;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.openrepose.commons.config.resource.ConfigurationResource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class TemplatingJaxbConfigurationParser<T> extends JaxbConfigurationParser<T> {

    private static final String START_OUTPUT_TAG = "{$";
    private static final String END_OUTPUT_TAG = "$}";
    private static final EnvironmentConfiguration ENV_CONF = EnvironmentConfigurationBuilder
        .configuration()
            .parser()
                .syntax()
                    .withStartOutput(START_OUTPUT_TAG).withEndOutput(END_OUTPUT_TAG)
                .and()
            .and()
        .build();

    public TemplatingJaxbConfigurationParser(Class<T> configurationClass, JAXBContext jaxbContext, URL xsdStreamSource) {
        super(configurationClass, jaxbContext, xsdStreamSource);
    }

    public TemplatingJaxbConfigurationParser(Class<T> configurationClass, URL xsdStreamSource, ClassLoader loader) throws JAXBException {
        super(configurationClass, xsdStreamSource, loader);
    }

    @Override
    public T read(ConfigurationResource cr) {
        return super.read(new ConfigurationResource() {
            @Override
            public boolean updated() throws IOException {
                return cr.updated();
            }

            @Override
            public boolean exists() throws IOException {
                return cr.exists();
            }

            @Override
            public String name() {
                return cr.name();
            }

            @Override
            public InputStream newInputStream() throws IOException {
                return reifyConf(cr.newInputStream());
            }
        });
    }

    private InputStream reifyConf(InputStream rawConf) throws IOException {
        JtwigModel model = JtwigModel.newModel(Collections.unmodifiableMap(System.getenv()));

        // TODO: Handle character encoding of XML configuration file
        // TODO: DocumentBuilder normally handles this, but we want to reify the template before parsing, so we cannot (easily) continue relying on it
        // TODO: XML spec for determining the character encoding https://www.w3.org/TR/REC-xml/#sec-guessing
        String template = IOUtils.toString(rawConf, StandardCharsets.UTF_8);
        String result = JtwigTemplate.inlineTemplate(template, ENV_CONF).render(model);

        return new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8));
    }
}
