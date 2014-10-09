package org.openrepose.filters.translation.xslt.xmlfilterchain;

import org.openrepose.filters.translation.xslt.XsltException;
import org.openrepose.filters.translation.xslt.XsltParameter;

import javax.xml.transform.sax.SAXTransformerFactory;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class XmlFilterChain {

    private final SAXTransformerFactory factory;
    private final List<XmlFilterReference> filters;

    public XmlFilterChain(SAXTransformerFactory factory, List<XmlFilterReference> filters) {
        this.factory = factory;
        this.filters = filters;
    }

    public SAXTransformerFactory getFactory() {
        return factory;
    }

    public List<XmlFilterReference> getFilters() {
        return filters;
    }

    public void executeChain(InputStream in, OutputStream output, List<XsltParameter> inputs, List<XsltParameter<? extends OutputStream>> outputs) throws XsltException {
        if (in == null || output == null) {
            return;
        }
        new XmlFilterChainExecutor(this).executeChain(in, output, inputs, outputs);
    }
}
