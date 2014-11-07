package org.openrepose.filters.ratelimiting.util.combine;

import org.apache.commons.pool.ObjectPool;
import org.openrepose.commons.utils.transform.StreamTransform;
import org.openrepose.commons.utils.transform.xslt.AbstractXslTransform;
import org.openrepose.commons.utils.transform.xslt.XsltTransformationException;
import org.openrepose.services.ratelimit.config.Limits;
import org.openrepose.services.ratelimit.config.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(CombinedLimitsTransformer.class);
    private final JAXBContext jaxbContext;
    private final ObjectFactory factory;
    
    public CombinedLimitsTransformer(Templates templates, JAXBContext jaxbContext, ObjectFactory factory) {
        super(templates);
        
        this.jaxbContext = jaxbContext;
        this.factory = factory;
    }
    
    @Override
    public void transform(final LimitsTransformPair source, final OutputStream target) {
        Transformer pooledObject;
        final ObjectPool<Transformer> objectPool = getXslTransformerPool();
        try {
            pooledObject = objectPool.borrowObject();
            try {
                final InputStreamUriParameter inputStreamUriParameter = new InputStreamUriParameter(source.getInputStream());
                final StreamResult resultWriter = new StreamResult(target);
                // The XSL requires a parameter to represent the absolute limits.
                // This harness cheats and provides the input stream directly.
                pooledObject.setURIResolver(inputStreamUriParameter);
                pooledObject.setParameter("absoluteURL", inputStreamUriParameter.getHref());
                final Limits limitsObject = new Limits();
                limitsObject.setRates(source.getRateLimitList());
                pooledObject.transform(new JAXBSource(jaxbContext, factory.createLimits(limitsObject)), resultWriter);
            } catch (Exception e) {
                objectPool.invalidateObject(pooledObject);
                pooledObject = null;
                throw new XsltTransformationException("Failed while attempting XSLT transformation.", e);
            } finally {
                if (pooledObject != null) {
                    objectPool.returnObject(pooledObject);
                }
            }
        } catch (XsltTransformationException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to obtain a Transformer", e);
        }
    }
}
