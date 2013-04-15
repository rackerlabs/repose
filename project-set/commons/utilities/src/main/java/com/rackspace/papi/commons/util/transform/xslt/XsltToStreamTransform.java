package com.rackspace.papi.commons.util.transform.xslt;

import com.rackspace.papi.commons.util.pooling.Pool;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import com.rackspace.papi.commons.util.pooling.SimpleResourceContext;
import com.rackspace.papi.commons.util.transform.StreamTransform;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;

public class XsltToStreamTransform<T extends OutputStream> implements StreamTransform<JAXBElement, T> {

    private final Pool<Transformer> xsltResourcePool;
    private final Templates transformationTemplates;
    private final JAXBContext jaxbContext;
    private final XsltTransformConstruction construction;

    public XsltToStreamTransform(Templates transformTemplates, JAXBContext jaxbContext) {
        this.construction = new XsltTransformConstruction();
        this.transformationTemplates = transformTemplates;
        this.jaxbContext = jaxbContext;

        xsltResourcePool = construction.generateXsltResourcePool(transformationTemplates);
    }

    @Override
    public void transform(final JAXBElement source, final T target) {
        xsltResourcePool.use(new SimpleResourceContext<Transformer>() {

            @Override
            public void perform(Transformer resource) throws ResourceContextException {
                try {
                    resource.transform(new JAXBSource(jaxbContext, source), new StreamResult(target));
                } catch (Exception e) {
                    throw new XsltTransformationException("Failed while attempting XSLT transformation;. Reason: "
                                                                  + e.getMessage(), e);
                }
            }
        });
    }
}
