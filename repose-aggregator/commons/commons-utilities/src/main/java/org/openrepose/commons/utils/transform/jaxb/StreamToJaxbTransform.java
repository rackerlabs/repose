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

import org.apache.commons.pool.ObjectPool;
import org.openrepose.commons.utils.pooling.ResourceContextException;
import org.openrepose.commons.utils.transform.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;

public class StreamToJaxbTransform<T> extends AbstractJaxbTransform implements Transform<InputStream, JAXBElement<T>> {

    private static final Logger LOG = LoggerFactory.getLogger(JaxbEntityToXml.class);

    public StreamToJaxbTransform(JAXBContext jc) {
        super(jc);
    }

    @Override
    public JAXBElement<T> transform(final InputStream source) {
        JAXBElement<T> rtn = null;
        Unmarshaller pooledObject;
        final ObjectPool<Unmarshaller> objectPool = getUnmarshallerPool();
        try {
            pooledObject = objectPool.borrowObject();
            try {
                rtn = (JAXBElement<T>) pooledObject.unmarshal(source);
            } catch (JAXBException jbe) {
                objectPool.invalidateObject(pooledObject);
                pooledObject = null;
                throw new ResourceContextException(jbe.getMessage(), jbe);
            } catch (Exception e) {
                objectPool.invalidateObject(pooledObject);
                pooledObject = null;
                LOG.error("Failed to utilize the Marshaller. Reason: {}", e.getLocalizedMessage());
                LOG.trace("", e);
            } finally {
                if (pooledObject != null) {
                    objectPool.returnObject(pooledObject);
                }
            }
        } catch (ResourceContextException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to obtain a Marshaller. Reason: {}", e.getLocalizedMessage());
            LOG.trace("", e);
        }
        return rtn;
    }
}
