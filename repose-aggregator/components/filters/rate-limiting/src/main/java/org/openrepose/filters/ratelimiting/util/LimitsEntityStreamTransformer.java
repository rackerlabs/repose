package org.openrepose.filters.ratelimiting.util;

import org.openrepose.commons.utils.transform.StreamTransform;
import org.openrepose.commons.utils.transform.jaxb.JaxbToStreamTransform;
import org.openrepose.commons.utils.transform.xslt.StreamToXsltTransform;
import org.openrepose.filters.ratelimiting.util.combine.CombinedLimitsTransformer;
import org.openrepose.filters.ratelimiting.util.combine.LimitsTransformPair;
import org.openrepose.services.ratelimit.config.Limits;
import org.openrepose.services.ratelimit.config.ObjectFactory;

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
