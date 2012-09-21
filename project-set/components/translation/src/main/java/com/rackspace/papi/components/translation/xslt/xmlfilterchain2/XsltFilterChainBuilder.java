package com.rackspace.papi.components.translation.xslt.xmlfilterchain2;

import com.rackspace.papi.components.translation.xslt.AbstractXsltChainBuilder;
import com.rackspace.papi.components.translation.xslt.StyleSheetInfo;
import com.rackspace.papi.components.translation.xslt.TransformReference;
import com.rackspace.papi.components.translation.xslt.XsltChain;
import com.rackspace.papi.components.translation.xslt.XsltException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class XsltFilterChainBuilder extends AbstractXsltChainBuilder<Templates>  {

    public XsltFilterChainBuilder(SAXTransformerFactory factory) {
        super(factory);
    }

    @Override
    public XsltChain<Templates> build(StyleSheetInfo... stylesheets) throws XsltException {
        List<TransformReference<Templates>> handlers = new ArrayList<TransformReference<Templates>>();
        

        try {
            for (StyleSheetInfo resource : stylesheets) {
                StreamSource source = getStylesheetSource(resource);
                handlers.add(new TransformReference<Templates>(resource.getId(), getFactory().newTemplates(source)));
            }
        } catch (TransformerConfigurationException ex) {
            throw new XsltException(ex);
        }

        return new XsltFilterChain(getFactory(), handlers);
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
