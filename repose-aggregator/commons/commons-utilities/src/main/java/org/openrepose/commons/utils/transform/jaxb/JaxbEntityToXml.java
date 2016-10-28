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
import org.openrepose.commons.utils.pooling.ResourceConstructionException;
import org.openrepose.commons.utils.transform.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

public class JaxbEntityToXml extends AbstractJaxbTransform implements Transform<JAXBElement, String> {

    private static final Logger LOG = LoggerFactory.getLogger(JaxbEntityToXml.class);

    public JaxbEntityToXml(JAXBContext ctx) {
        super(ctx);
    }

    @Override
    public String transform(final JAXBElement source) {
        String rtn = null;
        try {
            rtn = doTransform(getMarshallerPool(), source);
        } catch (ResourceConstructionException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to obtain a Marshaller. Reason: {}", e.getLocalizedMessage());
            LOG.trace("", e);
        }
        return rtn;
    }

    private String doTransform(final ObjectPool<Marshaller> objectPool, final JAXBElement source) throws Exception {
        String rtn = null;
        Marshaller pooledObject = objectPool.borrowObject();
        try (StringWriter w = new StringWriter()) {
            pooledObject.marshal(source, w);
            rtn = w.getBuffer().toString();
        } catch (JAXBException jaxbe) {
            objectPool.invalidateObject(pooledObject);
            pooledObject = null;
            throw new ResourceConstructionException(jaxbe.getMessage(), jaxbe);
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
        return rtn;
    }
}
