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
package org.openrepose.filters.ratelimiting.util;

import org.openrepose.commons.utils.logging.ExceptionLogger;
import org.openrepose.commons.utils.xslt.LogErrorListener;
import org.openrepose.commons.utils.xslt.LogTemplatesWrapper;
import org.openrepose.core.servlet.PowerApiContextException;
import org.slf4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;

/**
 * @author zinic
 */
public final class TransformHelper {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(TransformHelper.class);
    private static final ExceptionLogger EXCEPTION_LOG = new ExceptionLogger(LOG);

    /**
     * This is the class that was used in the original repose before having to set the classloader
     * I don't know if this is the right class, but it does work.
     */
    private static final TransformerFactory XSLT_TRANSFORMER_FACTORY =
            TransformerFactory.newInstance("org.apache.xalan.processor.TransformerFactoryImpl", TransformHelper.class.getClassLoader());

    static {
        XSLT_TRANSFORMER_FACTORY.setErrorListener(new LogErrorListener());
    }

    private TransformHelper() {

    }

    private static Templates parseXslt(Source s) throws TransformerConfigurationException {
        synchronized (XSLT_TRANSFORMER_FACTORY) {
            return new LogTemplatesWrapper(XSLT_TRANSFORMER_FACTORY.newTemplates(s));
        }
    }

    public static JAXBContext buildJaxbContext(Class... objectFactories) {
        try {
            return JAXBContext.newInstance(objectFactories);
        } catch (Exception e) {
            throw EXCEPTION_LOG.newException("Unable to build Rate Limiter. Reason: " + e.getMessage(),
                    e, PowerApiContextException.class);
        }
    }

    public static Templates getTemplatesFromInputStream(InputStream iStream) {
        try {
            return TransformHelper.parseXslt(new StreamSource(iStream));
        } catch (TransformerConfigurationException tce) {
            throw EXCEPTION_LOG.newException("Failed to generate new transform templates",
                    tce, RuntimeException.class);
        }
    }
}
