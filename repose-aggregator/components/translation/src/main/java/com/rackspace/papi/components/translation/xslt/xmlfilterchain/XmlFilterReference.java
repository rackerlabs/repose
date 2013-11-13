package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import org.slf4j.Logger;
import org.xml.sax.XMLReader;

public class XmlFilterReference {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(XmlFilterReference.class);
    private final String id;
    private final XMLReader reader;

    public XmlFilterReference(String id, XMLReader reader) {
        LOG.info("Translation style sheet " + id + " using XmlFilter of type: " + reader.getClass().getCanonicalName());
        this.id = id;
        this.reader = reader;
    }

    public String getId() {
        return id;
    }

    public XMLReader getReader() {
        return reader;
    }
}
