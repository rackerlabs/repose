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

import org.openrepose.filters.translation.xslt.XsltException;
import org.openrepose.filters.translation.xslt.XsltParameter;

import javax.xml.transform.sax.SAXTransformerFactory;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class XmlFilterChain {

    private final SAXTransformerFactory factory;
    private final List<XmlFilterReference> filters;

    public XmlFilterChain(SAXTransformerFactory factory, List<XmlFilterReference> filters) {
        this.factory = factory;
        this.filters = filters;
    }

    public SAXTransformerFactory getFactory() {
        return factory;
    }

    public List<XmlFilterReference> getFilters() {
        return filters;
    }

    public void executeChain(InputStream in, OutputStream output, List<XsltParameter> inputs, List<XsltParameter<? extends OutputStream>> outputs) throws XsltException {
        if (in == null || output == null) {
            return;
        }
        new XmlFilterChainExecutor(this).executeChain(in, output, inputs, outputs);
    }
}
