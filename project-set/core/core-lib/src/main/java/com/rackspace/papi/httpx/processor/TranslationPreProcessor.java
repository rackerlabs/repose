package com.rackspace.papi.httpx.processor;

import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.httpx.processor.cdata.UnknownContentStreamProcessor;
import com.rackspace.papi.httpx.processor.common.InputStreamProcessor;
import com.rackspace.papi.httpx.processor.json.JsonxStreamProcessor;
import com.rackspace.papi.httpx.processor.util.BodyContentMediaType;
import org.codehaus.jackson.JsonFactory;

import javax.xml.transform.sax.SAXTransformerFactory;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.transform.TransformerFactory;

public class TranslationPreProcessor {

    private static final SAXTransformerFactory HANDLER_FACTORY = (SAXTransformerFactory) TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
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
