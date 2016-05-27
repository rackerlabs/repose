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
package org.openrepose.filters.translation.httpx.marshaller;

import org.openrepose.core.httpx.MessageEnvelope;
import org.openrepose.filters.translation.httpx.ObjectFactoryUser;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * @author fran
 */
public class MessageEnvelopeMarshaller extends ObjectFactoryUser implements org.openrepose.filters.translation.httpx.marshaller.Marshaller<MessageEnvelope> {
    private static final String HTTPX_SCHEMA_LOCATION = "http://docs.rackspace.com/httpx/v1.0 ./httpx.xsd";

    @Override
    public InputStream marshall(MessageEnvelope messageEnvelope) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance("org.openrepose.core.httpx", this.getClass().getClassLoader());

            javax.xml.bind.Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_SCHEMA_LOCATION, HTTPX_SCHEMA_LOCATION);

            marshaller.marshal(getObjectFactory().createHttpx(messageEnvelope), outputStream);
        } catch (JAXBException e) {
            throw new MarshallerException("An exception occurred when attempting to marshal the http message envelope. Reason: " + e.getMessage(), e);
        }

        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}
