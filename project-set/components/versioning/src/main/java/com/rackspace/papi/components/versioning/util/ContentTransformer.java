package com.rackspace.papi.components.versioning.util;

import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.transform.StreamTransform;
import com.rackspace.papi.commons.util.transform.Transform;
import com.rackspace.papi.commons.util.transform.jaxb.JaxbEntityToXml;
import com.rackspace.papi.commons.util.transform.jaxb.JaxbToStreamTransform;
import com.rackspace.papi.commons.util.transform.xslt.JaxbXsltToStringTransform;
import com.rackspace.papi.commons.util.transform.xslt.XsltToStreamTransform;
import com.rackspace.papi.commons.util.xslt.TemplatesFactory;
import com.rackspace.papi.servlet.PowerApiContextException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.transform.Templates;
import java.io.OutputStream;

public class ContentTransformer {

    private static final String JSON_XSLT = "/META-INF/transform/xslt/version-json.xsl";
    
    @Deprecated
    private final Transform<JAXBElement, String> jsonTransform;
    @Deprecated
    private final Transform<JAXBElement, String> xmlTransform;
    
    private final StreamTransform<JAXBElement, OutputStream> jsonStreamTransform;
    private final StreamTransform<JAXBElement, OutputStream> xmlStreamTransform;

    public ContentTransformer() {
        try {
            final JAXBContext context = JAXBContext.newInstance(
                    com.rackspace.papi.components.versioning.schema.ObjectFactory.class,
                    com.rackspace.papi.components.versioning.config.ObjectFactory.class);
            final Templates jsonXsltTemplates =
                    TemplatesFactory.instance().parseXslt(getClass().getResourceAsStream(JSON_XSLT));

            xmlTransform = new JaxbEntityToXml(context);
            jsonTransform = new JaxbXsltToStringTransform(jsonXsltTemplates, context);
            
            xmlStreamTransform = new JaxbToStreamTransform<OutputStream>(context);
            jsonStreamTransform = new XsltToStreamTransform<OutputStream>(jsonXsltTemplates, context);
        } catch (Exception ex) {
            throw new PowerApiContextException(
                    "Failed to build transformation processors for response marshalling. Reason: "
                    + ex.getMessage(), ex);
        }
    }

    public void transform(JAXBElement element, MediaType mediaRange, OutputStream outputStream) {
        switch (mediaRange.getMimeType()) {
            case APPLICATION_XML:
                xmlStreamTransform.transform(element, outputStream);
                break;

            case APPLICATION_JSON:
            case UNKNOWN:
            default:
                jsonStreamTransform.transform(element, outputStream);
        }
    }

    @Deprecated
    public String transform(JAXBElement element, MediaType mediaRange) {
        switch (mediaRange.getMimeType()) {
            case APPLICATION_XML:
                return xmlTransform.transform(element);

            case APPLICATION_JSON:
            case UNKNOWN:
            default:
                return jsonTransform.transform(element);
        }
    }
}
