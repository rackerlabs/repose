package org.openrepose.filters.translation.xslt.xmlfilterchain
import org.openrepose.filters.translation.TranslationHandler
import org.openrepose.filters.translation.xslt.XsltParameter
import net.sf.saxon.Configuration
import net.sf.saxon.Controller
import net.sf.saxon.Filter
import net.sf.saxon.value.TextFragmentValue
import spock.lang.Specification

import javax.xml.transform.Transformer
import javax.xml.transform.sax.SAXTransformerFactory

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
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

    def "Execute Chain for Saxon will remove expected documents"() {
        given:
        XmlFilterChainExecutor executor = new XmlFilterChainExecutor(chain)
        controller.getDocumentPool().add(new TextFragmentValue("butts", "a uri"), "a uri")
        inputs.add(new XsltParameter(TranslationHandler.INPUT_HEADERS_URI, "a uri"))
        when:
        executor.executeChain(input, out, inputs, outputs)
        then:
        controller.getDocumentPool().find("a uri") == null
    }

    def "Execute Chain for Saxon will not remove expected documents"() {
        given:
        XmlFilterChainExecutor executor = new XmlFilterChainExecutor(chain)
        controller.getDocumentPool().add(new TextFragmentValue("butts", "a uri"), "a uri")
        inputs.add(new XsltParameter("butts", "a uri"))
        when:
        executor.executeChain(input, out, inputs, outputs)
        then:
        controller.getDocumentPool().find("a uri")
    }
}
