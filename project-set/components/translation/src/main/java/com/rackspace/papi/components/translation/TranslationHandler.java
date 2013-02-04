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
import com.rackspace.papi.commons.util.pooling.ResourceContext;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.translation.httpx.HttpxMarshaller;
import com.rackspace.papi.components.translation.xslt.XsltException;
import com.rackspace.papi.components.translation.xslt.XsltParameter;
import com.rackspace.papi.components.translation.xslt.xmlfilterchain.XmlChainPool;
import com.rackspace.papi.components.translation.xslt.xmlfilterchain.XmlFilterChain;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.httpx.processor.TranslationPreProcessor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.openrepose.repose.httpx.v1.Headers;
import org.openrepose.repose.httpx.v1.NameValuePair;
import org.openrepose.repose.httpx.v1.QualityNameValuePair;
import org.openrepose.repose.httpx.v1.QueryParameters;

public class TranslationHandler extends AbstractFilterLogicHandler {

  private static final int DEFAULT_BUFFER_SIZE = 2048;
  private static final MediaType DEFAULT_TYPE = new MediaType(MimeType.WILDCARD);
  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TranslationHandler.class);
  private static final String HEADERS_OUTPUT = "headers.xml";
  private static final String QUERY_OUTPUT = "query.xml";
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

  private static class ExecutionResult {

    private final boolean success;
    private final List<XsltParameter<? extends OutputStream>> outputs;
    private final HttpxMarshaller marshaller;

    ExecutionResult(boolean success) {
      this(success, null);
    }

    ExecutionResult(boolean success, List<XsltParameter<? extends OutputStream>> outputs) {
      this.success = success;
      this.outputs = outputs;
      this.marshaller = new HttpxMarshaller();
    }

    public boolean isSuccess() {
      return success;
    }

    private <T extends OutputStream> T getStream(String name) {
      if (outputs == null) {
        return null;
      }

      for (XsltParameter<? extends OutputStream> output : outputs) {
        if (name.equalsIgnoreCase(output.getName())) {
          return (T) output.getValue();
        }
      }

      return null;
    }

    public <T extends OutputStream> T getHeaders() {
      return getStream(HEADERS_OUTPUT);
    }

    public <T extends OutputStream> T getParams() {
      return getStream(QUERY_OUTPUT);
    }

    public void applyResults(final FilterDirector director) {
      applyHeaders(director);
      applyQueryParams(director);
    }
    
    private void applyQueryParams(final FilterDirector director) {
      ByteArrayOutputStream paramsOutput = getParams();
      
      if (paramsOutput == null) {
        return;
      }

      ByteArrayInputStream input = new ByteArrayInputStream(paramsOutput.toByteArray());
      if (input.available() == 0) {
        return;
      }

      QueryParameters params = marshaller.unmarshallQueryParameters(input);
      
      if (params.getParameter() != null) {
        StringBuilder sb = new StringBuilder();
        
        for (NameValuePair param: params.getParameter()) {
          sb.append(param.getName()).append("=").append(param.getValue() != null? param.getValue(): "");
        }
        
        director.setRequestUriQuery(sb.toString());
      }
    }

    private void applyHeaders(final FilterDirector director) {
      ByteArrayOutputStream headersOutput = getHeaders();
      if (headersOutput == null) {
        return;
      }

      ByteArrayInputStream input = new ByteArrayInputStream(headersOutput.toByteArray());
      if (input.available() == 0) {
        return;
      }

      Headers headers = marshaller.unmarshallHeaders(input);

      if (headers.getRequest() != null) {

        director.requestHeaderManager().removeAllHeaders();

        for (QualityNameValuePair header : headers.getRequest().getHeader()) {
          director.requestHeaderManager().appendHeader(header.getName(), header.getValue(), header.getQuality());
        }
      }

      if (headers.getResponse() != null) {
        director.responseHeaderManager().removeAllHeaders();

        for (QualityNameValuePair header : headers.getResponse().getHeader()) {
          director.responseHeaderManager().appendHeader(header.getName(), header.getValue(), header.getQuality());
        }
      }
    }
  }

  private List<XsltParameter> getInputParameters(final MutableHttpServletRequest request, final MutableHttpServletResponse response) {
    List<XsltParameter> inputs = new ArrayList<XsltParameter>();
    inputs.add(new XsltParameter("request", request));
    inputs.add(new XsltParameter("response", response));
    inputs.add(new XsltParameter("requestId", request.getRequestId()));

    return inputs;
  }

  private List<XsltParameter<? extends OutputStream>> getOutputParameters() {
    ByteArrayOutputStream headers = new ByteArrayOutputStream();
    ByteArrayOutputStream queryParams = new ByteArrayOutputStream();
    List<XsltParameter<? extends OutputStream>> outputs = new ArrayList<XsltParameter<? extends OutputStream>>();
    outputs.add(new XsltParameter<OutputStream>(HEADERS_OUTPUT, headers));
    outputs.add(new XsltParameter<OutputStream>(QUERY_OUTPUT, queryParams));

    return outputs;
  }

  private ExecutionResult executePool(final XmlChainPool pool, final InputStream in, final OutputStream out, final List<XsltParameter> inputs) {
    ExecutionResult result = (ExecutionResult) pool.getPool().use(new ResourceContext<XmlFilterChain, ExecutionResult>() {
      @Override
      public ExecutionResult perform(XmlFilterChain chain) {

        inputs.addAll(pool.getParams());
        List<XsltParameter<? extends OutputStream>> outputs = getOutputParameters();

        try {
          chain.executeChain(in, out, inputs, outputs);
        } catch (XsltException ex) {
          LOG.warn("Error processing transforms", ex.getMessage());
          return new ExecutionResult(false);
        }
        return new ExecutionResult(true, outputs);
      }
    });

    return result;

  }

  private List<MediaType> getAcceptValues(List<HeaderValue> values) {
    MediaRangeProcessor processor = new MediaRangeProcessor(values);
    return processor.process();
  }

  private MediaType getContentType(String contentType) {
    MimeType contentMimeType = MimeType.getMatchingMimeType(contentType);
    return new MediaType(contentMimeType);
  }

  @Override
  public FilterDirector handleResponse(HttpServletRequest httpRequest, ReadableHttpServletResponse httpResponse) {
    MutableHttpServletRequest request = MutableHttpServletRequest.wrap(httpRequest);
    MutableHttpServletResponse response = MutableHttpServletResponse.wrap(httpRequest, httpResponse);
    final FilterDirector filterDirector = new FilterDirectorImpl();
    filterDirector.setFilterAction(FilterAction.PASS);
    MediaType contentType = getContentType(response.getHeader("content-type"));
    List<MediaType> acceptValues = getAcceptValues(request.getPreferredHeaders("Accept", DEFAULT_TYPE));
    XmlChainPool pool = getHandlerChainPool("", contentType, acceptValues, String.valueOf(response.getStatus()), responseProcessors);

    if (pool != null) {
      try {
        filterDirector.setResponseStatusCode(response.getStatus());
        if (response.hasBody()) {
          InputStream in = response.getBufferedOutputAsInputStream();
          if (in.available() > 0) {
            ExecutionResult result = executePool(
                    pool,
                    new TranslationPreProcessor(response.getInputStream(), contentType, true).getBodyStream(),
                    filterDirector.getResponseOutputStream(),
                    getInputParameters(request, response));

            if (result.isSuccess()) {
              result.applyResults(filterDirector);
              response.setContentType(pool.getResultContentType());
            } else {
              filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
              response.setContentLength(0);
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
    MediaType contentType = getContentType(request.getHeader("content-type"));
    List<MediaType> acceptValues = getAcceptValues(request.getPreferredHeaders("Accept", DEFAULT_TYPE));
    XmlChainPool pool = getHandlerChainPool(request.getMethod(), contentType, acceptValues, "", requestProcessors);

    if (pool != null) {
      try {
        final ByteBuffer internalBuffer = new CyclicByteBuffer(DEFAULT_BUFFER_SIZE, true);
        ExecutionResult result = executePool(
                pool,
                new TranslationPreProcessor(request.getInputStream(), contentType, true).getBodyStream(),
                new ByteBufferServletOutputStream(internalBuffer),
                getInputParameters(request, response));

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
