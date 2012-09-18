package com.rackspace.papi.components.translation.xslt;

public interface XsltChainBuilder<T> {

    XsltChain<T> build(StyleSheetInfo... stylesheets) throws XsltException;
    
}
