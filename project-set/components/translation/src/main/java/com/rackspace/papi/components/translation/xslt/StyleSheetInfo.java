package com.rackspace.papi.components.translation.xslt;

import org.w3c.dom.Node;

public class StyleSheetInfo {
    private final String id;
    private final String uri;
    private final Node xsl;
    private final String systemId;

    public StyleSheetInfo(String id, String uri) {
        this.id = id;
        this.uri = uri;
        this.xsl = null;
        this.systemId = null;
    }

    public StyleSheetInfo(String id, Node xsl, String systemId) {
        this.id = id;
        this.uri = null;
        this.xsl = xsl;
        this.systemId = systemId;
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
    
    public String getSystemId() {
        return systemId;
    }
}
