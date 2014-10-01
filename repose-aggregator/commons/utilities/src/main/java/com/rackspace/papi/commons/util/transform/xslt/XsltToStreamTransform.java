package com.rackspace.papi.commons.util.transform.xslt;

import com.rackspace.com.papi.components.checker.util.TransformPool$;
import com.rackspace.papi.commons.util.transform.StreamTransform;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;

public class XsltToStreamTransform<T extends OutputStream> implements StreamTransform<JAXBElement, T> {

    private final Templates transformationTemplates;
    private final JAXBContext jaxbContext;

    public XsltToStreamTransform(Templates transformTemplates, JAXBContext jaxbContext) {
        this.transformationTemplates = transformTemplates;
        this.jaxbContext = jaxbContext;
    }

    @Override
    public void transform(final JAXBElement source, final T target) {
        Transformer transformer = TransformPool$.MODULE$.borrowTransformer(transformationTemplates);
        try {
            transformer.transform(new JAXBSource(jaxbContext, source), new StreamResult(target));
        } catch (Exception e) {
            throw new XsltTransformationException("Failed while attempting XSLT transformation;. Reason: "
                                                          + e.getMessage(), e);
        }
        TransformPool$.MODULE$.returnTransformer(transformationTemplates, transformer);
    }
}
