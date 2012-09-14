package com.rackspace.papi.components.translation.xslt.handlerchain;

public class StyleSheetInfo {
    private final String id;
    private final String uri;

    public StyleSheetInfo(String id, String uri) {
        this.id = id;
        this.uri = uri;
    }

    public String getId() {
        return id;
    }

    public String getUri() {
        return uri;
    }
}
