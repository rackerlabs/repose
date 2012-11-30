package com.rackspace.papi.components.translation.xslt;

import org.w3c.dom.Node;

public class StyleSheetInfo {
    private final String id;
    private final String uri;
    private final Node xsl;

    public StyleSheetInfo(String id, String uri) {
        this.id = id;
        this.uri = uri;
        this.xsl = null;
    }

    public StyleSheetInfo(String id, Node xsl) {
        this.id = id;
        this.uri = null;
        this.xsl = xsl;
    }

    public String getId() {
        return id;
    }

    public String getUri() {
        return uri;
    }
    
    public Node getXsl() {
        return xsl;
    }
}
