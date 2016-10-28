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
package org.openrepose.filters.ratelimiting.util.combine;

import org.apache.commons.pool.ObjectPool;
import org.openrepose.commons.utils.transform.StreamTransform;
import org.openrepose.commons.utils.transform.xslt.AbstractXslTransform;
import org.openrepose.commons.utils.transform.xslt.XsltTransformationException;
import org.openrepose.core.services.ratelimit.config.Limits;
import org.openrepose.core.services.ratelimit.config.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;

/**
 * @author zinic
 */
public class CombinedLimitsTransformer extends AbstractXslTransform implements StreamTransform<LimitsTransformPair, OutputStream> {

    private static final Logger LOG = LoggerFactory.getLogger(CombinedLimitsTransformer.class);
    private final JAXBContext jaxbContext;
    private final ObjectFactory factory;

    public CombinedLimitsTransformer(Templates templates, JAXBContext jaxbContext, ObjectFactory factory) {
        super(templates);

        this.jaxbContext = jaxbContext;
        this.factory = factory;
    }

    @Override
    public void transform(final LimitsTransformPair source, final OutputStream target) {
        final ObjectPool<Transformer> objectPool = getXslTransformerPool();
        try {
            doTransform(source, target, objectPool, objectPool.borrowObject());
        } catch (XsltTransformationException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to obtain a Transformer. Reason: {}", e.getLocalizedMessage());
            LOG.trace("", e);
        }
    }

    private void doTransform(final LimitsTransformPair source,
                             final OutputStream target,
                             final ObjectPool<Transformer> objectPool,
                             Transformer pooledObject) throws Exception {
        try {
            final InputStreamUriParameter inputStreamUriParameter = new InputStreamUriParameter(source.getInputStream());
            final StreamResult resultWriter = new StreamResult(target);
            // The XSL requires a parameter to represent the absolute limits.
            // This harness cheats and provides the input stream directly.
            pooledObject.setURIResolver(inputStreamUriParameter);
            pooledObject.setParameter("absoluteURL", inputStreamUriParameter.getHref());
            final Limits limitsObject = new Limits();
            limitsObject.setRates(source.getRateLimitList());
            pooledObject.transform(new JAXBSource(jaxbContext, factory.createLimits(limitsObject)), resultWriter);
        } catch (Exception e) {
            objectPool.invalidateObject(pooledObject);
            pooledObject = null;
            throw new XsltTransformationException("Failed while attempting XSLT transformation.", e);
        } finally {
            if (pooledObject != null) {
                objectPool.returnObject(pooledObject);
            }
        }
    }
}
