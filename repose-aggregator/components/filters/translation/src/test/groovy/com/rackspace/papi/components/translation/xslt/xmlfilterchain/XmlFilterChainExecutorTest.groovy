package com.rackspace.papi.components.translation.xslt.xmlfilterchain

import com.rackspace.papi.components.translation.xslt.XsltParameter
import net.sf.saxon.lib.StandardURIResolver
import org.xml.sax.XMLReader
import spock.lang.Specification

import javax.xml.transform.sax.SAXTransformerFactory
import javax.xml.transform.Transformer
import net.sf.saxon.Filter
import net.sf.saxon.Controller
import net.sf.saxon.Configuration

import static org.mockito.Mockito.when
import static org.mockito.Mockito.mock

/**
 * Created by dimi5963 on 9/8/14.
 */
class XmlFilterChainExecutorTest extends Specification {

    SAXTransformerFactory factory;
    XmlFilterChain chain;
    InputStream input;
    OutputStream out;
    List<XsltParameter> inputs;
    List<XsltParameter<? extends OutputStream>> outputs;
    Transformer transformer;
    Filter filter;
    Controller controller;
    Configuration configuration;

    def setup(){

        factory = mock(SAXTransformerFactory.class)

        controller = new Controller(new Configuration())
        filter = new Filter(controller);
        List<XmlFilterReference> xmlFilterReferenceList = new ArrayList<XmlFilterReference>();
        xmlFilterReferenceList.add(new XmlFilterReference("some thing", filter))
        chain = new XmlFilterChain(factory, xmlFilterReferenceList)
        input = mock(InputStream.class)
        out = mock(OutputStream.class)
        inputs = new ArrayList();
        outputs = new ArrayList();
        transformer = mock(Transformer.class)
        when(chain.getFactory().newTransformer()).thenReturn(transformer)

    }

    def "Execute Chain for Saxon"() {
        given:
        XmlFilterChainExecutor executor = new XmlFilterChainExecutor(chain)
        when:
        executor.executeChain(input, out, inputs, outputs)
        then:
        controller.getDocumentPool().find("some thing") == null
    }
}
