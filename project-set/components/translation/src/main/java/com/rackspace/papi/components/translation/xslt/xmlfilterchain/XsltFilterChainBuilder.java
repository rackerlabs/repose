package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import com.rackspace.papi.components.translation.xslt.AbstractChainBuilder;
import com.rackspace.papi.components.translation.xslt.StyleSheetInfo;
import com.rackspace.papi.components.translation.xslt.TransformReference;
import com.rackspace.papi.components.translation.xslt.XsltChain;
import com.rackspace.papi.components.translation.xslt.XsltException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

public class XsltFilterChainBuilder extends AbstractChainBuilder<XMLFilter>  {

    public XsltFilterChainBuilder(SAXTransformerFactory factory) {
        super(factory);
    }

    @Override
    public XsltChain<XMLFilter> build(StyleSheetInfo... stylesheets) throws XsltException {
        try {
            List<TransformReference<XMLFilter>> filters = new ArrayList<TransformReference<XMLFilter>>();
            XMLReader lastReader = getSaxReader();

            for (StyleSheetInfo resource : stylesheets) {
                StreamSource source = getStylesheetSource(resource);
                // Wire the output of the reader to filter1 (see Note #3)
                // and the output of filter1 to filter2
                XMLFilter filter = getFactory().newXMLFilter(source);
                filter.setParent(lastReader);
                filters.add(new TransformReference(resource.getId(), filter));
                lastReader = filter;
            }

            return new XsltFilterChain(getFactory(), filters);
        } catch (TransformerConfigurationException ex) {
            throw new XsltException(ex);
        } catch (ParserConfigurationException ex) {
            throw new XsltException(ex);
        } catch (SAXException ex) {
            throw new XsltException(ex);
        }

    }


    protected XMLReader getSaxReader() throws ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setValidating(true);
        spf.setNamespaceAware(true);
        SAXParser parser = spf.newSAXParser();
        XMLReader reader = parser.getXMLReader();

        return reader;
    }
}
