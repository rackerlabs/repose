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
package org.openrepose.filters.translation.xslt.xmlfilterchain

import net.sf.saxon.Configuration
import net.sf.saxon.Controller
import net.sf.saxon.Filter
import net.sf.saxon.value.TextFragmentValue
import org.openrepose.filters.translation.TranslationFilter
import org.openrepose.filters.translation.xslt.XsltParameter
import spock.lang.Specification

import javax.xml.transform.Transformer
import javax.xml.transform.sax.SAXTransformerFactory

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

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

    def setup() {

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
        inputs.add(new XsltParameter(TranslationFilter.INPUT_HEADERS_URI, "a uri"))
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
