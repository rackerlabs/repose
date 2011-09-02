package com.rackspace.papi.components.ratelimit.util;

import com.rackspace.papi.commons.util.logging.ExceptionLogger;
import com.rackspace.papi.servlet.PowerApiContextException;
import org.slf4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;

/**
 *
 * @author zinic
 */
public class TransformHelper {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(TransformHelper.class);
    private static final ExceptionLogger EXCEPTION_LOG = new ExceptionLogger(LOG);
    private static final TransformerFactory XSLT_TRANSFORMER_FACTORY = TransformerFactory.newInstance();

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
            return XSLT_TRANSFORMER_FACTORY.newTemplates(new StreamSource(iStream));
        } catch (TransformerConfigurationException tce) {
            throw EXCEPTION_LOG.newException("Failed to generate new transform templates",
                    tce, RuntimeException.class);
        }
    }    
}
