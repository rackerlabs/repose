package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.components.translation.httpx.HttpxMarshaller;
import com.rackspace.papi.components.translation.xslt.XsltParameter;
import com.rackspace.papi.filter.logic.FilterDirector;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import org.openrepose.repose.httpx.v1.Headers;
import org.openrepose.repose.httpx.v1.NameValuePair;
import org.openrepose.repose.httpx.v1.QualityNameValuePair;
import org.openrepose.repose.httpx.v1.QueryParameters;
import org.openrepose.repose.httpx.v1.RequestInformation;

public class TranslationResult {

  public static final String HEADERS_OUTPUT = "headers.xml";
  public static final String QUERY_OUTPUT = "query.xml";
  public static final String REQUEST_OUTPUT = "request.xml";
  
  private final boolean success;
  private final List<XsltParameter<? extends OutputStream>> outputs;
  private final HttpxMarshaller marshaller;

  TranslationResult(boolean success) {
    this(success, null);
  }

  TranslationResult(boolean success, List<XsltParameter<? extends OutputStream>> outputs) {
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
  
  public <T extends OutputStream> T getRequestInfo() {
    return getStream(REQUEST_OUTPUT);
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
    applyRequestInfo(director);
  }
  
  private void applyRequestInfo(final FilterDirector director) {
    ByteArrayOutputStream requestOutput = getRequestInfo();

    if (requestOutput == null) {
      return;
    }

    ByteArrayInputStream input = new ByteArrayInputStream(requestOutput.toByteArray());
    if (input.available() == 0) {
      return;
    }

    RequestInformation requestInfo = marshaller.unmarshallRequestInformation(input);
    
    if(StringUtilities.isNotBlank(requestInfo.getUri())) {
      director.setRequestUri(requestInfo.getUri());
    }
    
    if (StringUtilities.isNotBlank(requestInfo.getUrl())) {
      director.setRequestUrl(new StringBuffer(requestInfo.getUrl()));
    }
    
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

      for (NameValuePair param : params.getParameter()) {
        sb.append(param.getName()).append("=").append(param.getValue() != null ? param.getValue() : "");
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
