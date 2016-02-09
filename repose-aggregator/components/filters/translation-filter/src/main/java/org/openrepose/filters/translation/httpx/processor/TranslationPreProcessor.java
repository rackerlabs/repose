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
package org.openrepose.filters.translation.httpx.processor;

import com.fasterxml.jackson.core.JsonFactory;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.filters.translation.httpx.processor.cdata.UnknownContentStreamProcessor;
import org.openrepose.filters.translation.httpx.processor.common.InputStreamProcessor;
import org.openrepose.filters.translation.httpx.processor.json.JsonxStreamProcessor;
import org.openrepose.filters.translation.httpx.processor.util.BodyContentMediaType;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import java.io.IOException;
import java.io.InputStream;

public class TranslationPreProcessor {

    private static final SAXTransformerFactory HANDLER_FACTORY = (SAXTransformerFactory) TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", TranslationPreProcessor.class.getClassLoader());
    private final boolean jsonPreprocessing;
    private final MediaType contentType;
    private final InputStream input;

    public TranslationPreProcessor(InputStream input, MediaType contentType, boolean jsonPreprocessing) {
        this.input = input;
        this.jsonPreprocessing = jsonPreprocessing;
        this.contentType = contentType;
    }

    public InputStream getBodyStream() throws IOException {
        final InputStream result;

        switch (BodyContentMediaType.getMediaType(contentType.getMimeType().getMimeType())) {
            case XML:
                result = input;
                break;
            case JSON:
                if (jsonPreprocessing) {
                    result = getJsonProcessor().process(input);
                } else {
                    result = input;
                }
                break;
            default:
                result = getUnknownContentProcessor().process(input);
                break;
        }

        return result;
    }

    protected InputStreamProcessor getJsonProcessor() {
        return new JsonxStreamProcessor(new JsonFactory(), HANDLER_FACTORY);
    }

    protected InputStreamProcessor getUnknownContentProcessor() {
        return new UnknownContentStreamProcessor();
    }
}
