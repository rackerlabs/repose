package com.rackspace.papi.components.translation.resolvers;

import com.rackspace.papi.components.translation.httpx.HttpxMarshaller;
import com.rackspace.papi.components.translation.httpx.HttpxProducer;
import org.openrepose.repose.httpx.v1.Headers;
import org.openrepose.repose.httpx.v1.QueryParameters;
import org.openrepose.repose.httpx.v1.RequestInformation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

public class HttpxUriInputParameterResolver extends SourceUriResolver {

  public static final String HEADERS_PREFIX = "repose:input:headers";
  public static final String PARAMS_PREFIX = "repose:input:query";
  public static final String REQUEST_INFO_PREFIX = "repose:input:request";
  private HttpServletRequest request;
  private HttpServletResponse response;
  private HttpxProducer producer;
  private Headers headers;

  public void setHeaders(Headers headers) {
    this.headers = headers;
  }

  public void setParams(QueryParameters params) {
    this.params = params;
  }

  public void setRequestInformation(RequestInformation info) {
    this.info = info;
  }

  private QueryParameters params;
  private RequestInformation info;
  private final HttpxMarshaller marshaller;
 

  public HttpxUriInputParameterResolver() {
    super();
  
    marshaller = new HttpxMarshaller();
  }

  public HttpxUriInputParameterResolver(URIResolver parent) {
    super(parent);
    marshaller = new HttpxMarshaller();
  }
  
  public void reset() {
    request = null;
    response = null;
    producer = null;
    headers = null;
    params = null;
    info = null;
  }

  public void setRequest(HttpServletRequest request) {
    this.request = request;
  }

  public void setResponse(HttpServletResponse response) {
    this.response = response;
  }

  private HttpxProducer getProducer() {
      if (producer == null) {
        producer = new HttpxProducer(request, response);
      }
      
      return producer;
  }

  private Headers getHeaders() {
    return headers != null? headers :  getProducer().getHeaders();
  }

  private RequestInformation getRequestInformation() {
    return info != null? info :  getProducer().getRequestInformation();
  }

  private QueryParameters getRequestParameters() {
    return params != null? params :  getProducer().getRequestParameters();
  }

    @Override
    public Source resolve(String href, String base) throws TransformerException {

        if (href != null) {
            if (href.toLowerCase().startsWith(HEADERS_PREFIX)) {
                return new StreamSource(marshaller.marshall(getHeaders()));
            } else if (href.toLowerCase().startsWith(REQUEST_INFO_PREFIX)) {
                return new StreamSource(marshaller.marshall(getRequestInformation()));
            } else if (href.toLowerCase().startsWith(PARAMS_PREFIX)) {
                return new StreamSource(marshaller.marshall(getRequestParameters()));
            }
        }
        return super.resolve(href, base);
    }
}
