package com.rackspace.papi.components.translation.xslt.handlerchain;

import com.rackspace.papi.components.translation.xslt.AbstractXsltChainBuilder;
import com.rackspace.papi.components.translation.xslt.StyleSheetInfo;
import com.rackspace.papi.components.translation.xslt.TransformReference;
import com.rackspace.papi.components.translation.xslt.XsltChain;
import com.rackspace.papi.components.translation.xslt.XsltException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

public class XsltHandlerChainBuilder extends AbstractXsltChainBuilder<Templates> {

    public XsltHandlerChainBuilder(SAXTransformerFactory factory) {
        super(factory);
    }

    public XsltChain<Templates> build(StyleSheetInfo... stylesheets) throws XsltException {
        List<TransformReference<Templates>> handlers = new ArrayList<TransformReference<Templates>>();
        TransformReference<TransformerHandler> lastHandler = null;

        try {
            for (StyleSheetInfo resource : stylesheets) {
                StreamSource source = getStylesheetSource(resource);
                handlers.add(new TransformReference<Templates>(resource.getId(), getFactory().newTemplates(source)));
            }
        } catch (TransformerConfigurationException ex) {
            throw new XsltException(ex);
        }

        return new XsltHandlerChain(getFactory(), handlers);
    }

}
