package com.rackspace.papi.commons.util.transform.xslt;

import com.rackspace.com.papi.components.checker.util.TransformPool$;
import com.rackspace.papi.commons.util.transform.Transform;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class JaxbXsltToStringTransform implements Transform<JAXBElement, String> {

    private final Templates transformationTemplates;
    private final JAXBContext jaxbContext;

    public JaxbXsltToStringTransform(Templates transformTemplates, JAXBContext jaxbContext) {
        this.transformationTemplates = transformTemplates;
        this.jaxbContext = jaxbContext;
    }

    @Override
    public String transform(final JAXBElement source) {
        Transformer transformer = TransformPool$.MODULE$.borrowTransformer(transformationTemplates);
        final StringWriter stringWriter = new StringWriter();
        final StreamResult resultWriter = new StreamResult(stringWriter);

        try {
            transformer.transform(new JAXBSource(jaxbContext, source), resultWriter);
        } catch (Exception e) {
            throw new XsltTransformationException("Failed while attempting XSLT transformation;. Reason: "
                                                          + e.getMessage(), e);
        }
        TransformPool$.MODULE$.returnTransformer(transformationTemplates, transformer);

        return stringWriter.getBuffer().toString();
    }
}
