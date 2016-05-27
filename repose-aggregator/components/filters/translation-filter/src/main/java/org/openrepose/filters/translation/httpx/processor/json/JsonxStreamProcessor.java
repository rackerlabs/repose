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
package org.openrepose.filters.translation.httpx.processor.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.openrepose.commons.utils.Destroyable;
import org.openrepose.commons.utils.thread.DestroyableThreadWrapper;
import org.openrepose.filters.translation.httpx.processor.common.Element;
import org.openrepose.filters.translation.httpx.processor.common.InputStreamProcessor;
import org.openrepose.filters.translation.httpx.processor.common.PreProcessorException;
import org.openrepose.filters.translation.httpx.processor.json.elements.ElementFactory;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Properties;

public class JsonxStreamProcessor implements InputStreamProcessor {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(JsonxStreamProcessor.class);
    private static final String JSON_URI = "http://www.ibm.com/xmlns/prod/2009/jsonx";
    private static final String XSD_URI = "http://www.w3.org/2001/XMLSchema";
    private static final String JSON_PREFIX = "json";
    private static final String XSD_PREFIX = "xsd";
    private final JsonFactory jsonFactory;
    private final SAXTransformerFactory handlerFactory;
    private final Properties format;
    private DestroyableThreadWrapper processingThread;

    public JsonxStreamProcessor(JsonFactory jsonFactory, SAXTransformerFactory handlerFactory) {
        this.jsonFactory = jsonFactory;
        this.handlerFactory = handlerFactory;
        this.processingThread = null;
        format = new Properties();
        format.put(OutputKeys.METHOD, "xml");
        format.put(OutputKeys.OMIT_XML_DECLARATION, "no");
        format.put(OutputKeys.ENCODING, "UTF-8");
        format.put(OutputKeys.INDENT, "no");
    }

    public JsonxStreamProcessor(JsonFactory jsonFactory, SAXTransformerFactory handlerFactory, Properties properties) {
        this.jsonFactory = jsonFactory;
        this.handlerFactory = handlerFactory;
        format = properties;
    }

    @Override
    public InputStream process(InputStream sourceStream) throws PreProcessorException {
        try {
            final TransformerHandler transformerHandler = handlerFactory.newTransformerHandler();
            final PipedInputStream resultStream = new PipedInputStream();
            final PipedOutputStream out = new PipedOutputStream(resultStream);
            transformerHandler.getTransformer().setOutputProperties(format);
            transformerHandler.setResult(new StreamResult(out));

            processingThread = DestroyableThreadWrapper.newThread(new JsonStreamProcessor(transformerHandler, sourceStream, out));
            processingThread.start();

            return resultStream;
        } catch (IOException ex) {
            throw new PreProcessorException(ex);
        } catch (TransformerConfigurationException ex) {
            throw new PreProcessorException(ex);
        }
    }

    private class JsonStreamProcessor implements Runnable, Destroyable {

        private final JsonParser jp;
        private final TransformerHandler handler;
        private final OutputStream out;
        private boolean exitThread = false;

        public JsonStreamProcessor(TransformerHandler handler, InputStream jsonIn, OutputStream out) throws IOException {
            this.handler = handler;
            this.jp = jsonFactory.createJsonParser(jsonIn);
            this.out = out;
        }

        private void startDocument() throws SAXException {
            handler.startDocument();
            handler.startPrefixMapping(JSON_PREFIX, JSON_URI);
            handler.startPrefixMapping(XSD_PREFIX, XSD_URI);
        }

        private void endDocument() throws SAXException {
            try {
                handler.endPrefixMapping(XSD_PREFIX);
            } catch (Exception ex) {
                LOG.warn("Unable to end prefix mapping: " + XSD_PREFIX, ex);
            }
            try {
                handler.endPrefixMapping(JSON_PREFIX);
            } catch (Exception ex) {
                LOG.warn("Unable to end prefix mapping: " + JSON_PREFIX, ex);
            }
            handler.endDocument();
        }

        @Override
        public void run() {
            try {
                startDocument();

                try {
                    while (jp.nextToken() != null && !exitThread) {
                        outputItem(jp, handler);
                    }
                } finally {
                    endDocument();
                }

            } catch (Exception ex) {
                LOG.error("Error processing JSON input stream. Reason: " + ex.getMessage(), ex);
            } finally {
                try {
                    out.close();
                } catch (IOException ex) {
                    LOG.warn("Unable to close output stream", ex);
                }
            }

        }

        private void outputItem(JsonParser jp, ContentHandler handler) throws IOException, SAXException {
            JsonToken token = jp.getCurrentToken();
            String fieldName = jp.getCurrentName();

            if (token.isScalarValue()) {
                if (token.isNumeric()) {
                    ElementFactory.getScalarElement(token.name(), fieldName, jp.getNumberValue()).outputElement(handler);
                } else {
                    ElementFactory.getScalarElement(token.name(), fieldName, jp.getText()).outputElement(handler);
                }
            } else {
                Element element = ElementFactory.getElement(token.name(), fieldName);
                if (element != null) {
                    element.outputElement(handler);
                }
            }
        }

        @Override
        public void destroy() {
            exitThread = true;
        }
    }
}
