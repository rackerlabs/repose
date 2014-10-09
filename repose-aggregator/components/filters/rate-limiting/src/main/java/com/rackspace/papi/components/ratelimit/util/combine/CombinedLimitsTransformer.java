package com.rackspace.papi.components.ratelimit.util.combine;

import org.openrepose.commons.utils.pooling.SimpleResourceContext;
import org.openrepose.commons.utils.transform.StreamTransform;
import org.openrepose.commons.utils.transform.xslt.AbstractXslTransform;
import org.openrepose.commons.utils.transform.xslt.XsltTransformationException;

import com.rackspace.repose.service.limits.schema.Limits;
import com.rackspace.repose.service.limits.schema.ObjectFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;

/**
 *
 * @author zinic
 */
public class CombinedLimitsTransformer extends AbstractXslTransform implements StreamTransform<LimitsTransformPair, OutputStream> {

    private final JAXBContext jaxbContext;
    private final ObjectFactory factory;
    
    public CombinedLimitsTransformer(Templates templates, JAXBContext jaxbContext, ObjectFactory factory) {
        super(templates);
        
        this.jaxbContext = jaxbContext;
        this.factory = factory;
    }
    
    @Override
    public void transform(final LimitsTransformPair source, final OutputStream target) {
        final InputStreamUriParameter inputStreamUriParameter = new InputStreamUriParameter(source.getInputStream());
     
        getXslTransformerPool().use(new SimpleResourceContext<Transformer>() {

            @Override
            public void perform(Transformer resource) {
                final StreamResult resultWriter = new StreamResult(target);                
                // The XSL requires a parameter to represent the absolute limits.
                // This harness cheats and provides the input stream directly.
                resource.setURIResolver(inputStreamUriParameter);
                resource.setParameter("absoluteURL", inputStreamUriParameter.getHref());
                try {
                    final Limits limitsObject = new Limits();
                    limitsObject.setRates(source.getRateLimitList());
                    resource.transform(new JAXBSource(jaxbContext, factory.createLimits(limitsObject)), resultWriter);
                } catch (Exception e) {
                    throw new XsltTransformationException("Failed while attempting XSLT transformation;. Reason: "
                            + e.getMessage(), e);
                }
            }
        });
    }
}
