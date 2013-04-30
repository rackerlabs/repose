package com.rackspace.papi.commons.util.transform.xslt;

import com.rackspace.papi.commons.util.pooling.Pool;
import com.rackspace.papi.commons.util.pooling.ResourceContext;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import com.rackspace.papi.commons.util.transform.Transform;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class JaxbXsltToStringTransform implements Transform<JAXBElement, String> {

    private final Pool<Transformer> xsltResourcePool;
    private final Templates transformationTemplates;
    private final JAXBContext jaxbContext;
    private final XsltTransformConstruction construction;

    public JaxbXsltToStringTransform(Templates transformTemplates, JAXBContext jaxbContext) {
        this.construction = new XsltTransformConstruction();
        this.transformationTemplates = transformTemplates;
        this.jaxbContext = jaxbContext;

        xsltResourcePool = construction.generateXsltResourcePool(transformationTemplates);
    }

    @Override
    public String transform(final JAXBElement source) {
        return xsltResourcePool.use(new ResourceContext<Transformer, String>() {

            @Override
            public String perform(Transformer resource) throws ResourceContextException {
                final StringWriter stringWriter = new StringWriter();
                final StreamResult resultWriter = new StreamResult(stringWriter);

                try {
                    resource.transform(new JAXBSource(jaxbContext, source), resultWriter);
                } catch (Exception e) {
                    throw new XsltTransformationException("Failed while attempting XSLT transformation;. Reason: "
                                                                  + e.getMessage(), e);
                }

                return stringWriter.getBuffer().toString();
            }
        });
    }
}
