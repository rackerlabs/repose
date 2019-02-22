/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.translation.xslt.xmlfilterchain;

import net.sf.saxon.lib.FeatureKeys;
import org.openrepose.filters.translation.xslt.StyleSheetInfo;
import org.openrepose.filters.translation.xslt.XsltException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class XmlFilterChainBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(XmlFilterChainBuilder.class);
    private static final String CLASSPATH_PREFIX = "classpath://";
    //
    // TODO: SAXON_WORKAROUND
    // A bug in SaxonEE v9.4.0.9 causes a license to be required for IdentityTransformer, even when it is not required.
    // This was supposed to be fixed in v9.5, but is still present in v9.5.1-8.
    // Until this is fixed in a version we can use, we will use the XalanC Transformer in place of SaxonHE for Identity transforms.
    // This same procedure is used in api-checker to get around this issue.
    //
    private static final String XALANC_FACTORY_NAME = "org.apache.xalan.xsltc.trax.TransformerFactoryImpl";
    private static SAXTransformerFactory xalancTransformerFactory;

    static {
        try {
            xalancTransformerFactory = (SAXTransformerFactory) TransformerFactory.newInstance(XALANC_FACTORY_NAME, XmlFilterChainBuilder.class.getClassLoader());
            xalancTransformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (TransformerConfigurationException ex) {
            LOG.error("Error", ex);
        }
    }

    private final SAXTransformerFactory factory;
    private final boolean allowEntities;
    private final boolean allowDtdDeclarations;

    public XmlFilterChainBuilder(SAXTransformerFactory factory, boolean allowEntities, boolean allowDeclarations) {
        this.factory = factory;
        this.allowEntities = allowEntities;
        this.allowDtdDeclarations = allowDeclarations;
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS, Boolean.TRUE);
        } catch (TransformerConfigurationException ex) {
            LOG.error("Error", ex);
        }
    }

    public XmlFilterChain build(StyleSheetInfo... stylesheets) throws XsltException {
        try {
            List<XmlFilterReference> filters = new ArrayList<>();
            XMLReader lastReader = getSaxReader();

            if (stylesheets.length > 0) {
                for (StyleSheetInfo resource : stylesheets) {
                    Source source = getStylesheetSource(resource);
                    // Wire the output of the reader to filter1 (see Note #3)
                    // and the output of filter1 to filter2
                    XMLFilter filter = doBuild(resource, source);
                    filter.setParent(lastReader);
                    filters.add(new XmlFilterReference(resource.getId(), filter));
                    lastReader = filter;
                }
            } else {
                filters.add(new XmlFilterReference(null, lastReader));
            }

            return new XmlFilterChain(xalancTransformerFactory, filters);
        } catch (ParserConfigurationException | SAXException ex) {
            throw new XsltException(ex);
        }
    }

    private XMLFilter doBuild(StyleSheetInfo resource, Source source) {
        try {
            return factory.newXMLFilter(source);
        } catch (TransformerConfigurationException ex) {
            LOG.error("Error creating XML Filter for " + resource.getUri(), ex);
            throw new XsltException(ex);
        }
    }

    protected StreamSource getClassPathResource(String path) {
        String resource = path.substring(CLASSPATH_PREFIX.length());
        InputStream input = getClass().getResourceAsStream(resource);
        URL inputURL = getClass().getResource(resource);
        if (input != null) {
            return new StreamSource(input, inputURL.toExternalForm());
        }

        throw new XsltException("Unable to load stylesheet " + path);
    }

    private StreamSource nodeToStreamSource(Node node, String systemId) {
        try {
            // Create dom source for the document
            DOMSource domSource = new DOMSource(node, systemId);

            // Create a string writer
            StringWriter stringWriter = new StringWriter();

            // Create the result stream for the transform
            StreamResult result = new StreamResult(stringWriter);

            // Create a Transformer to serialize the document
            Transformer transformer = xalancTransformerFactory.newTransformer();

            // Transform the document to the result stream
            transformer.transform(domSource, result);
            StringReader reader = new StringReader(stringWriter.toString());
            return new StreamSource(reader);
        } catch (TransformerException ex) {
            throw new XsltException(ex);
        }
    }

    protected Source getStylesheetSource(StyleSheetInfo stylesheet) {
        if (stylesheet.getXsl() != null) {
            return nodeToStreamSource(stylesheet.getXsl(), stylesheet.getSystemId());
        } else if (stylesheet.getUri() != null) {
            if (stylesheet.getUri().startsWith(CLASSPATH_PREFIX)) {
                return getClassPathResource(stylesheet.getUri());
            } else {
                try {
                    URL stylesheetURL = new URL(stylesheet.getUri());
                    return new StreamSource(stylesheetURL.openStream(), stylesheetURL.toExternalForm());
                } catch (IOException ex) {
                    throw new XsltException("Unable to load stylesheet: " + stylesheet.getUri(), ex);
                }
            }
        }
        throw new IllegalArgumentException("No stylesheet specified for " + stylesheet.getId());
    }

    protected XMLReader getSaxReader() throws ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setXIncludeAware(false);
        spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        spf.setValidating(true);
        spf.setNamespaceAware(true);
        spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !allowDtdDeclarations);
        SAXParser parser = spf.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        reader.setEntityResolver(new ReposeEntityResolver(reader.getEntityResolver(), allowEntities));

        LOG.info("SAXParserFactory class: " + spf.getClass().getCanonicalName());
        LOG.info("SAXParser class: " + parser.getClass().getCanonicalName());
        LOG.info("XMLReader class: " + reader.getClass().getCanonicalName());

        return reader;
    }
}
