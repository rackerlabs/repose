package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.media.MediaRangeProcessor;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.io.ByteBufferInputStream;
import com.rackspace.papi.commons.util.io.ByteBufferServletOutputStream;
import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;
import com.rackspace.papi.commons.util.io.buffer.CyclicByteBuffer;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.translation.xslt.XsltParameter;
import com.rackspace.papi.components.translation.xslt.xmlfilterchain.TranslationResult;
import com.rackspace.papi.components.translation.xslt.xmlfilterchain.XmlChainPool;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.httpx.processor.TranslationPreProcessor;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class TranslationHandler extends AbstractFilterLogicHandler {

  private static final int DEFAULT_BUFFER_SIZE = 2048;
  private static final MediaType DEFAULT_TYPE = new MediaType(MimeType.WILDCARD);
  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TranslationHandler.class);
  private final List<XmlChainPool> requestProcessors;
  private final List<XmlChainPool> responseProcessors;

  public TranslationHandler(List<XmlChainPool> requestProcessors, List<XmlChainPool> responseProcessors) {
    this.requestProcessors = requestProcessors;
    this.responseProcessors = responseProcessors;
  }

  List<XmlChainPool> getRequestProcessors() {
    return requestProcessors;
  }

  List<XmlChainPool> getResponseProcessors() {
    return responseProcessors;
  }

  private XmlChainPool getHandlerChainPool(String method, MediaType contentType, List<MediaType> accept, String status, List<XmlChainPool> pools) {
    for (MediaType value : accept) {
      for (XmlChainPool pool : pools) {
        if (pool.accepts(method, contentType, value, status)) {
          return pool;
        }
      }
    }

    return null;
  }
  
  private enum TranslationType {
    REQUEST,
    RESPONSE
  }

  private List<XsltParameter> getInputParameters(final TranslationType type, final MutableHttpServletRequest request, final MutableHttpServletResponse response) {
    List<XsltParameter> inputs = new ArrayList<XsltParameter>();
    inputs.add(new XsltParameter("request", request));
    inputs.add(new XsltParameter("response", response));
    inputs.add(new XsltParameter("requestId", request.getRequestId()));
    inputs.add(new XsltParameter("responseId", response.getResponseId()));
    
    final String id;
    if (type == TranslationType.REQUEST) {
      id = request.getRequestId();
    } else {
      id = response.getResponseId();
    }
    /* Input/Ouput URIs */
    inputs.add(new XsltParameter("input-headers-uri", "repose:input:headers:" + id));
    inputs.add(new XsltParameter("input-query-uri", "repose:input:query:" + id));
    inputs.add(new XsltParameter("input-request-uri", "repose:input:request:" + id));
    inputs.add(new XsltParameter("output-headers-uri", "repose:output:headers.xml"));
    inputs.add(new XsltParameter("output-query-uri", "repose:output:query.xml"));
    inputs.add(new XsltParameter("output-request-uri", "repose:output:request.xml"));

    return inputs;
  }

  private List<MediaType> getAcceptValues(List<HeaderValue> values) {
    MediaRangeProcessor processor = new MediaRangeProcessor(values);
    return processor.process();
  }

  private MediaType getContentType(HeaderValue contentType) {
    MimeType contentMimeType = MimeType.getMatchingMimeType(contentType != null? contentType.getValue(): "");
    return new MediaType(contentMimeType);
  }

  @Override
  public FilterDirector handleResponse(HttpServletRequest httpRequest, ReadableHttpServletResponse httpResponse) {
    MutableHttpServletRequest request = MutableHttpServletRequest.wrap(httpRequest);
    MutableHttpServletResponse response = MutableHttpServletResponse.wrap(httpRequest, httpResponse);
    final FilterDirector filterDirector = new FilterDirectorImpl();
    filterDirector.setFilterAction(FilterAction.PASS);
    MediaType contentType = getContentType(response.getHeaderValue("Content-Type"));
    List<MediaType> acceptValues = getAcceptValues(request.getPreferredHeaders("Accept", DEFAULT_TYPE));
    XmlChainPool pool = getHandlerChainPool("", contentType, acceptValues, String.valueOf(response.getStatus()), responseProcessors);

    if (pool != null) {
      try {
        filterDirector.setResponseStatusCode(response.getStatus());
        if (response.hasBody()) {
          InputStream in = response.getBufferedOutputAsInputStream();
          if (in.available() > 0) {
            TranslationResult result = pool.executePool(
                    new TranslationPreProcessor(response.getInputStream(), contentType, true).getBodyStream(),
                    filterDirector.getResponseOutputStream(),
                    getInputParameters(TranslationType.RESPONSE, request, response));

            if (result.isSuccess()) {
              result.applyResults(filterDirector);
              filterDirector.responseHeaderManager().putHeader("Content-Type", pool.getResultContentType());
            } else {
              filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
              response.setContentLength(0);
              filterDirector.responseHeaderManager().removeHeader("Content-Length");
            }
          }
        }
      } catch (IOException ex) {
        LOG.error("Error executing response transformer chain", ex);
        filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
        response.setContentLength(0);
      }
    } else {
      filterDirector.setResponseStatusCode(response.getStatus());
    }

    return filterDirector;
  }

  @Override
  public FilterDirector handleRequest(HttpServletRequest httpRequest, ReadableHttpServletResponse httpResponse) {
    MutableHttpServletRequest request = MutableHttpServletRequest.wrap(httpRequest);
    MutableHttpServletResponse response = MutableHttpServletResponse.wrap(httpRequest, httpResponse);
    FilterDirector filterDirector = new FilterDirectorImpl();
    MediaType contentType = getContentType(request.getHeaderValue("content-type"));
    List<MediaType> acceptValues = getAcceptValues(request.getPreferredHeaders("Accept", DEFAULT_TYPE));
    XmlChainPool pool = getHandlerChainPool(request.getMethod(), contentType, acceptValues, "", requestProcessors);

    if (pool != null) {
      try {
        final ByteBuffer internalBuffer = new CyclicByteBuffer(DEFAULT_BUFFER_SIZE, true);
        TranslationResult result = pool.executePool(
                new TranslationPreProcessor(request.getInputStream(), contentType, true).getBodyStream(),
                new ByteBufferServletOutputStream(internalBuffer),
                getInputParameters(TranslationType.REQUEST, request, response));

        if (result.isSuccess()) {
          request.setInputStream(new ByteBufferInputStream(internalBuffer));
          result.applyResults(filterDirector);
          filterDirector.requestHeaderManager().putHeader("content-type", pool.getResultContentType());
          filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE);
        } else {
          filterDirector.setResponseStatus(HttpStatusCode.BAD_REQUEST);
          filterDirector.setFilterAction(FilterAction.RETURN);
        }
      } catch (IOException ex) {
        LOG.error("Error executing request transformer chain", ex);
        filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
        filterDirector.setFilterAction(FilterAction.RETURN);
      }
    } else {
      filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE);
    }


    return filterDirector;
  }
}
