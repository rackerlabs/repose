package com.rackspace.papi.components.translation.resolvers;

import com.rackspace.papi.components.translation.httpx.HttpxMarshaller;
import com.rackspace.papi.components.translation.httpx.HttpxProducer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
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

  @Override
  public Source resolve(String href, String base) throws TransformerException {
    
    if (href == null) {
      return super.resolve(href, base);
    }

    if (href.toLowerCase().startsWith(HEADERS_PREFIX)) {
       return new StreamSource(marshaller.marshall(producer.getHeaders()));
    } else if (href.toLowerCase().startsWith(REQUEST_INFO_PREFIX)) {
       return new StreamSource(marshaller.marshall(producer.getRequestInformation()));
    } else if (href.toLowerCase().startsWith(PARAMS_PREFIX)) {
       return new StreamSource(marshaller.marshall(producer.getRequestParameters()));
    }

    return super.resolve(href, base);
  }
}
