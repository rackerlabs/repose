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
package org.openrepose.commons.utils.transform.jaxb;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.openrepose.commons.utils.pooling.ResourceConstructionException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * @author zinic
 */
public abstract class AbstractJaxbTransform {

    private final ObjectPool<Marshaller> marshallerPool;
    private final ObjectPool<Unmarshaller> unmarshallerPool;
    private final JAXBContext jaxbContext;

    public AbstractJaxbTransform(JAXBContext ctx) {
        jaxbContext = ctx;

        marshallerPool = new SoftReferenceObjectPool<>(new BasePoolableObjectFactory<Marshaller>() {
            @Override
            public Marshaller makeObject() {
                try {
                    return jaxbContext.createMarshaller();
                } catch (JAXBException jaxbe) {
                    throw new ResourceConstructionException(jaxbe.getMessage(), jaxbe);
                }
            }
        });

        unmarshallerPool = new SoftReferenceObjectPool<>(new BasePoolableObjectFactory<Unmarshaller>() {
            @Override
            public Unmarshaller makeObject() {
                try {
                    return jaxbContext.createUnmarshaller();
                } catch (JAXBException jaxbe) {
                    throw new ResourceConstructionException(jaxbe.getMessage(), jaxbe);
                }
            }
        });
    }

    protected ObjectPool<Marshaller> getMarshallerPool() {
        return marshallerPool;
    }

    protected ObjectPool<Unmarshaller> getUnmarshallerPool() {
        return unmarshallerPool;
    }
}
