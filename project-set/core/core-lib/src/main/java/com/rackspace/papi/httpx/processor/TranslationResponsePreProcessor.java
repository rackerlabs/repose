package com.rackspace.papi.httpx.processor;

import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.httpx.processor.cdata.UnknownContentStreamProcessor;
import com.rackspace.papi.httpx.processor.common.InputStreamProcessor;
import com.rackspace.papi.httpx.processor.json.JsonxStreamProcessor;
import com.rackspace.papi.httpx.processor.util.BodyContentMediaType;
import org.codehaus.jackson.JsonFactory;

import javax.xml.transform.sax.SAXTransformerFactory;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.transform.TransformerFactory;

public class TranslationResponsePreProcessor {

    private static final SAXTransformerFactory HANDLER_FACTORY = (SAXTransformerFactory) TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
    private final MutableHttpServletResponse response;
    private final boolean jsonPreprocessing;
    private final MediaType contentType;

    public TranslationResponsePreProcessor(MutableHttpServletResponse response, MediaType contentType, boolean jsonPreprocessing) {
        this.response = response;
        this.jsonPreprocessing = jsonPreprocessing;
        this.contentType = contentType;
    }

    public InputStream getBodyStream() throws IOException {
        final InputStream result;

        switch (BodyContentMediaType.getMediaType(contentType.getMimeType().getSubType())) {
            case XML:
                result = response.getInputStream();
                break;
            case JSON:
                if (jsonPreprocessing) {
                    result = getJsonProcessor().process(response.getInputStream());
                } else {
                    result = response.getInputStream();
                }
                break;
            default:
                result = getUnknownContentProcessor().process(response.getInputStream());
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
