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

import net.sf.saxon.Controller;
import net.sf.saxon.lib.OutputURIResolver;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.DocumentPool;
import org.apache.xalan.transformer.TrAXFilter;
import org.openrepose.docs.repose.httpx.v1.Headers;
import org.openrepose.docs.repose.httpx.v1.QueryParameters;
import org.openrepose.docs.repose.httpx.v1.RequestInformation;
import org.openrepose.filters.translation.TranslationFilter;
import org.openrepose.filters.translation.resolvers.*;
import org.openrepose.filters.translation.xslt.XsltException;
import org.openrepose.filters.translation.xslt.XsltParameter;
import org.slf4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class XmlFilterChainExecutor {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(XmlFilterChainExecutor.class);
    private final XmlFilterChain chain;
    private final Properties format = new Properties();


    public XmlFilterChainExecutor(XmlFilterChain chain) {
        this.chain = chain;
        format.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
        format.put(OutputKeys.ENCODING, "UTF-8");
    }

    protected SourceUriResolverChain getResolverChain(Transformer transformer) {
        URIResolver resolver = transformer.getURIResolver();
        SourceUriResolverChain resolverChain;
        if (!(resolver instanceof SourceUriResolverChain)) {
            resolverChain = new SourceUriResolverChain(resolver);
            resolverChain.addResolver(new InputStreamUriParameterResolver());
            resolverChain.addResolver(new HttpxUriInputParameterResolver());
            resolverChain.addResolver(new ClassPathUriResolver());
            transformer.setURIResolver(resolverChain);
        } else {
            resolverChain = (SourceUriResolverChain) resolver;
        }
        return resolverChain;
    }

    protected OutputStreamUriParameterResolver getOutputUriResolver(Transformer transformer) {
        if (transformer instanceof Controller) {
            Controller controller = (Controller) transformer;
            OutputURIResolver resolver = controller.getOutputURIResolver();
            if (!(resolver instanceof OutputStreamUriParameterResolver)) {
                resolver = new OutputStreamUriParameterResolver(controller.getOutputURIResolver());
                controller.setOutputURIResolver(resolver);
            }
            return (OutputStreamUriParameterResolver) resolver;
        }
        return null;
    }

    protected void setInputParameters(String id, Transformer transformer, List<XsltParameter> inputs) {

        SourceUriResolverChain resolverChain = getResolverChain(transformer);
        InputStreamUriParameterResolver resolver = resolverChain.getResolverOfType(InputStreamUriParameterResolver.class);
        resolver.clearStreams();

        if (inputs != null && !inputs.isEmpty()) {

            HttpxUriInputParameterResolver headersResolver = resolverChain.getResolverOfType(HttpxUriInputParameterResolver.class);
            headersResolver.reset();

            for (XsltParameter input : inputs) {
                if ("*".equals(input.getStyleId()) || id != null && id.equals(input.getStyleId())) {
                    String param = null;
                    if (input.getValue() instanceof InputStream) {
                        param = resolver.addStream((InputStream) input.getValue());
                    } else if (input.getValue() instanceof HttpServletRequest) {
                        headersResolver.setRequest((HttpServletRequest) input.getValue());
                    } else if (input.getValue() instanceof HttpServletResponse) {
                        headersResolver.setResponse((HttpServletResponse) input.getValue());
                    } else if (input.getValue() instanceof Headers) {
                        headersResolver.setHeaders((Headers) input.getValue());
                    } else if (input.getValue() instanceof QueryParameters) {
                        headersResolver.setParams((QueryParameters) input.getValue());
                    } else if (input.getValue() instanceof RequestInformation) {
                        headersResolver.setRequestInformation((RequestInformation) input.getValue());
                    } else {
                        param = input.getValue() != null ? input.getValue().toString() : null;
                    }

                    if (param != null) {
                        transformer.setParameter(input.getName(), param);
                    }
                }
            }
        }
    }

    private void setAlternateOutputs(Transformer transformer, List<XsltParameter<? extends OutputStream>> outputs) {
        OutputStreamUriParameterResolver resolver = getOutputUriResolver(transformer);
        if (resolver != null) {
            resolver.clearStreams();

            if (outputs != null && !outputs.isEmpty()) {
                for (XsltParameter<? extends OutputStream> output : outputs) {
                    String paramName = resolver.addStream(output.getValue(), output.getName());
                    transformer.setParameter("headersOutputUri", paramName);
                }
            }
        }
    }

    public void executeChain(InputStream in, OutputStream output, List<XsltParameter> inputs, List<XsltParameter<? extends OutputStream>> outputs) throws XsltException {
        List<String> uris = findInputUris(inputs);
        try {
            for (XmlFilterReference filter : chain.getFilters()) {
                // pass the input stream to all transforms as a param inputstream

                Transformer transformer;
                if (filter.getReader() instanceof net.sf.saxon.Filter) {
                    net.sf.saxon.Filter saxonFilter = (net.sf.saxon.Filter) filter.getReader();
                    transformer = saxonFilter.getTransformer();
                } else if (filter.getReader() instanceof TrAXFilter) {
                    TrAXFilter traxFilter = (TrAXFilter) filter.getReader();
                    transformer = traxFilter.getTransformer();
                } else {
                    LOG.debug("Unable to set stylesheet parameters.  Unsupported xml filter type used: " + filter.getReader().getClass().getCanonicalName());
                    transformer = null;
                }

                if (transformer != null) {
                    transformer.clearParameters();
                    setInputParameters(filter.getId(), transformer, inputs);
                    setAlternateOutputs(transformer, outputs);
                }
            }

            Transformer transformer = chain.getFactory().newTransformer();
            transformer.setOutputProperties(format);
            transformer.transform(getSAXSource(new InputSource(in)), new StreamResult(output));

            //remove documents from cache
            for (XmlFilterReference filter : chain.getFilters()) {
                if (filter.getReader() instanceof net.sf.saxon.Filter) {
                    net.sf.saxon.Filter saxonFilter = (net.sf.saxon.Filter) filter.getReader();
                    Transformer filterTransformer = saxonFilter.getTransformer();
                    Controller controller = (Controller) filterTransformer;

                    removeInputUrisFromPool(controller.getDocumentPool(), uris);
                }
            }
        } catch (TransformerException ex) {
            throw new XsltException(ex);
        }
    }

    private List<String> findInputUris(List<XsltParameter> inputs) {
        List<String> uris = new ArrayList<>();
        for (XsltParameter parameter : inputs) {
            if (isInputUriName(parameter.getName())) {
                uris.add((String) parameter.getValue());
            }
        }
        return uris;
    }

    private boolean isInputUriName(String name) {
        return TranslationFilter.INPUT_HEADERS_URI.equals(name) ||
                TranslationFilter.INPUT_QUERY_URI.equals(name) ||
                TranslationFilter.INPUT_REQUEST_URI.equals(name);
    }

    private void removeInputUrisFromPool(DocumentPool documentPool, List<String> uris) {
        for (String uri : uris) {
            DocumentInfo documentInfo = documentPool.find(uri);
            if (documentInfo != null) {
                LOG.trace("Removing document {}", uri);
                documentPool.discard(documentInfo);
            } else {
                LOG.trace("Tried to remove document {} but wasn't present.", uri);
            }
        }
    }

    protected SAXSource getSAXSource(InputSource source) {
        if (chain.getFilters().isEmpty()) {
            return new SAXSource(source);
        }

        XMLReader lastFilter = chain.getFilters().get(chain.getFilters().size() - 1).getReader();

        return new SAXSource(lastFilter, source);
    }
}
