package com.rackspace.papi.components.ratelimit.util;

import com.rackspace.papi.commons.util.transform.StreamTransform;
import com.rackspace.papi.commons.util.transform.jaxb.JaxbToStreamTransform;
import com.rackspace.papi.commons.util.transform.xslt.StreamToXsltTransform;
import com.rackspace.repose.service.limits.schema.Limits;
import com.rackspace.repose.service.limits.schema.ObjectFactory;
import com.rackspace.papi.components.ratelimit.util.combine.CombinedLimitsTransformer;
import com.rackspace.papi.components.ratelimit.util.combine.LimitsTransformPair;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import java.io.InputStream;
import java.io.OutputStream;

public class LimitsEntityStreamTransformer {

    private static final ObjectFactory LIMITS_OBJECT_FACTORY = new ObjectFactory();

    public static final String JSON_XSL_LOCATION = "/META-INF/xslt/limits-json.xsl",
            COMBINER_XSL_LOCATION = "/META-INF/xslt/limits-combine.xsl";

    private final StreamTransform<LimitsTransformPair, OutputStream> combiner;
    private final StreamTransform<InputStream, OutputStream> jsonTransform;
    private final StreamTransform<JAXBElement<Limits>, OutputStream> entiyTransform;

    public LimitsEntityStreamTransformer() {
        this(buildJaxbContext());
    }

    public LimitsEntityStreamTransformer(JAXBContext context) {
        jsonTransform = new StreamToXsltTransform(
                TransformHelper.getTemplatesFromInputStream(
                        LimitsEntityStreamTransformer.class.getResourceAsStream(JSON_XSL_LOCATION)));

        combiner = new CombinedLimitsTransformer(
                TransformHelper.getTemplatesFromInputStream(
                        LimitsEntityStreamTransformer.class.getResourceAsStream(COMBINER_XSL_LOCATION)), context, LIMITS_OBJECT_FACTORY);

        entiyTransform = new JaxbToStreamTransform(context);
    }

    private static JAXBContext buildJaxbContext() {
        return TransformHelper.buildJaxbContext(LIMITS_OBJECT_FACTORY.getClass());
    }

    public void combine(LimitsTransformPair pair, OutputStream out) {
        combiner.transform(pair, out);
    }

    public void streamAsJson(InputStream in, OutputStream out) {
        jsonTransform.transform(in, out);
    }

    public void entityAsXml(Limits l, OutputStream output) {
        entiyTransform.transform(LIMITS_OBJECT_FACTORY.createLimits(l), output);
    }
}
