package features.filters.translation.httpx.processor;

import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import features.filters.translation.httpx.processor.cdata.UnknownContentStreamProcessor;
import features.filters.translation.httpx.processor.common.InputStreamProcessor;
import features.filters.translation.httpx.processor.json.JsonxStreamProcessor;
import features.filters.translation.httpx.processor.util.BodyContentMediaType;
import org.codehaus.jackson.JsonFactory;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import java.io.IOException;
import java.io.InputStream;

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
