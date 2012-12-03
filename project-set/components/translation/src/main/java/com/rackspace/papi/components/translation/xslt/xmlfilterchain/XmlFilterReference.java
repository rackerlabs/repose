package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import org.slf4j.Logger;
import org.xml.sax.XMLFilter;

public class XmlFilterReference {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(XmlFilterReference.class);
    private final String id;
    private final XMLFilter filter;

    public XmlFilterReference(String id, XMLFilter filter) {
        LOG.info("Translation style sheet " + id + " using XmlFilter of type: " + filter.getClass().getCanonicalName());
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
