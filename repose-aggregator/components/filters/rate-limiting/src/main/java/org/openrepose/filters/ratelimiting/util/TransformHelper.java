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
 *
 * @author zinic
 */
public final class TransformHelper {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(TransformHelper.class);
    private static final ExceptionLogger EXCEPTION_LOG = new ExceptionLogger(LOG);
    private static final TransformerFactory XSLT_TRANSFORMER_FACTORY =
            TransformerFactory.newInstance("javax.xml.transform.TransformerFactory", TransformHelper.class.getClassLoader());

    static {
        XSLT_TRANSFORMER_FACTORY.setErrorListener(new LogErrorListener());
    }

    private static Templates parseXslt(Source s) throws TransformerConfigurationException {
        synchronized (XSLT_TRANSFORMER_FACTORY) {
            return new LogTemplatesWrapper(XSLT_TRANSFORMER_FACTORY.newTemplates(s));
        }
    }

    private TransformHelper() {
        
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
