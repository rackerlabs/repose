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
package org.openrepose.commons.utils.transform.xslt;

import org.apache.commons.pool.ObjectPool;
import org.openrepose.commons.utils.transform.StreamTransform;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author zinic
 */
public class StreamToXsltTransform extends AbstractXslTransform implements StreamTransform<InputStream, OutputStream> {

    public StreamToXsltTransform(Templates transformationTemplates) {
        super(transformationTemplates);
    }

    @Override
    public void transform(final InputStream source, final OutputStream target) {
        final ObjectPool<Transformer> objectPool = getXslTransformerPool();
        try {
            doTransform(objectPool, source, target);
        } catch (XsltTransformationException e) {
            throw e;
        } catch (Exception e) {
            throw new XsltTransformationException("Failed to obtain a Transformer for XSLT transformation.", e);
        }
    }

    private void doTransform(final ObjectPool<Transformer> objectPool, final InputStream source, final OutputStream target) throws Exception {
        Transformer pooledObject = objectPool.borrowObject();
        try {
            pooledObject.transform(new StreamSource(source), new StreamResult(target));
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
