package com.rackspace.papi.components.translation.xslt.handlerchain;

import javax.xml.transform.sax.TransformerHandler;

public class XslTransformer {
    private final String id;
    private final TransformerHandler handler;
    
    public XslTransformer(String id, TransformerHandler handler) {
        this.id = id;
        this.handler = handler;
    }

    public String getId() {
        return id;
    }

    public TransformerHandler getHandler() {
        return handler;
    }
    
}
