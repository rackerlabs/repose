package com.rackspace.papi.components.translation.xslt;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import javax.xml.transform.sax.SAXTransformerFactory;

public interface XsltChain<T> {

    void executeChain(InputStream in, OutputStream output, List<XsltParameter> inputs, List<XsltParameter<? extends OutputStream>> outputs) throws XsltException;

    SAXTransformerFactory getFactory();

    List<TransformReference<T>> getFilters();
    
}
