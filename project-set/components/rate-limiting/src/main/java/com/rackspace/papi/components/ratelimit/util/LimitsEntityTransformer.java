package com.rackspace.papi.components.ratelimit.util;

import com.rackspace.papi.commons.util.transform.Transform;
import com.rackspace.papi.commons.util.transform.jaxb.JaxbEntityToXml;
import com.rackspace.papi.commons.util.transform.xslt.JaxbXsltToStringTransform;
import com.rackspace.repose.service.limits.schema.Limits;
import com.rackspace.repose.service.limits.schema.ObjectFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.transform.Templates;

/**
 *
 * @author jhopper
 */
public class LimitsEntityTransformer {
    private static final ObjectFactory LIMITS_OBJECT_FACTORY = new ObjectFactory();

    public static final String XSLT_LOCATION = "/META-INF/xslt/limits-json.xsl";
    
    private final Transform<JAXBElement, String> jsonTransform;
    private final Transform<JAXBElement, String> xmlTransform;

    public LimitsEntityTransformer() {
        this(buildJaxbContext());
    }

    public LimitsEntityTransformer(JAXBContext context) {
        jsonTransform = new JaxbXsltToStringTransform(getTemplates(), context);
        xmlTransform = new JaxbEntityToXml(context);
    }

    private static JAXBContext buildJaxbContext() {
        return TransformHelper.buildJaxbContext(LIMITS_OBJECT_FACTORY.getClass());
    }

    private Templates getTemplates() {
        return TransformHelper.getTemplatesFromInputStream(LimitsEntityTransformer.class.getResourceAsStream(XSLT_LOCATION));
    }

    private <T> String transform(Transform<T, String> t, T source) {
        return t.transform(source);
    }

    public String entityAsJson(Limits l) {
        return transform(jsonTransform, LIMITS_OBJECT_FACTORY.createLimits(l));
    }

    public String entityAsXml(Limits l) {
        return transform(xmlTransform, LIMITS_OBJECT_FACTORY.createLimits(l));
    }
}
