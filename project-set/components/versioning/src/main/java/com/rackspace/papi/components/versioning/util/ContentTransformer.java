package com.rackspace.papi.components.versioning.util;

import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import com.rackspace.papi.commons.util.transform.Transform;
import com.rackspace.papi.commons.util.transform.jaxb.JaxbEntityToXml;
import com.rackspace.papi.commons.util.transform.xslt.JaxbXsltTransform;
import com.rackspace.papi.commons.util.xslt.TemplatesFactory;
import com.rackspace.papi.servlet.ServletContextInitException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.transform.Templates;

/**
 *
 * @author jhopper
 */
public class ContentTransformer {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ContentTransformer.class);
    private static final ExceptionLogger EXCEPTION_LOG = new ExceptionLogger(LOG);
    public static final String JSON_XSLT = "/META-INF/transform/xslt/version-json.xsl";

    private final Transform<JAXBElement<?>, String> jsonTransform;
    private final Transform<JAXBElement<?>, String> xmlTransform;

    public ContentTransformer() {
        try {
            final JAXBContext context = JAXBContext.newInstance(
                    com.rackspace.papi.components.versioning.schema.ObjectFactory.class,
                    com.rackspace.papi.components.versioning.config.ObjectFactory.class);
            final Templates jsonXsltTemplates =
                    TemplatesFactory.instance().parseXslt(getClass().getResourceAsStream(JSON_XSLT));

            xmlTransform = new JaxbEntityToXml(context);
            jsonTransform = new JaxbXsltTransform(jsonXsltTemplates, context);
        } catch (Exception ex) {
            throw EXCEPTION_LOG.newException(
                    "Failed to build transformation processors for response marshalling. Reason: "
                    + ex.getMessage(), ex, ServletContextInitException.class);
        }
    }

    public String transform(JAXBElement element, MediaType mediaType) {
        switch (mediaType) {
            case APPLICATION_XML:
                return xmlTransform.transform(element);

            case APPLICATION_JSON:
            case UNKNOWN:
            default:
                return jsonTransform.transform(element);
        }
    }
}
