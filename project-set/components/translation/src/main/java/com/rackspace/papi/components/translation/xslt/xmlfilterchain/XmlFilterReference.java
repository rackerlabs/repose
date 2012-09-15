package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import org.xml.sax.XMLFilter;

public class XmlFilterReference {
    private final String id;
    private final XMLFilter filter;

    public XmlFilterReference(String id, XMLFilter filter) {
        this.id = id;
        this.filter = filter;
    }

    public String getId() {
        return id;
    }

    public XMLFilter getFilter() {
        return filter;
    }
}
