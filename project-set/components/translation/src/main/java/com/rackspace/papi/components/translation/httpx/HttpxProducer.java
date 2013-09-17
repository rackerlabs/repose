package com.rackspace.papi.components.translation.httpx;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.servlet.http.HeaderContainer;
import com.rackspace.papi.commons.util.servlet.http.RequestHeaderContainer;
import com.rackspace.papi.commons.util.servlet.http.ResponseHeaderContainer;
import org.openrepose.repose.httpx.v1.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class HttpxProducer {

  private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
  private final HttpServletRequest request;
  private final HttpServletResponse response;
  private Headers headers;
  private QueryParameters queryParameters;
  private RequestInformation requestInformation;

  public HttpxProducer(HttpServletRequest request, HttpServletResponse response) {
    this.request = request;
    this.response = response;
  }

  private HeaderList getHeaderList(HeaderContainer container) {
    HeaderList result = OBJECT_FACTORY.createHeaderList();
    List<QualityNameValuePair> headerList = result.getHeader();

    for (String name : container.getHeaderNames()) {
      List<HeaderValue> values = container.getHeaderValues(name);
      for (HeaderValue value : values) {
        QualityNameValuePair header = new QualityNameValuePair();

        header.setName(name);
        header.setValue(value.getValue());
        header.setQuality(value.getQualityFactor());

        headerList.add(header);
      }
    }

    return result;
  }
  
  public RequestInformation getRequestInformation() {
    if (requestInformation == null) {
      requestInformation = OBJECT_FACTORY.createRequestInformation();
      requestInformation.setUri(request.getRequestURI());
      requestInformation.setUrl(request.getRequestURL().toString());
      ReadOnlyRequestInformation info = OBJECT_FACTORY.createReadOnlyRequestInformation();
      
      info.setAuthType(StringUtilities.getValue(request.getAuthType(), ""));
      info.setContextPath(StringUtilities.getValue(request.getContextPath(), ""));
      info.setLocalAddr(StringUtilities.getValue(request.getLocalAddr(), ""));
      info.setLocalName(StringUtilities.getValue(request.getLocalName(), ""));
      info.setLocalPort(request.getLocalPort());
      info.setPathInfo(StringUtilities.getValue(request.getPathInfo(), ""));
      info.setPathTranslated(StringUtilities.getValue(request.getPathTranslated(), ""));
      info.setProtocol(StringUtilities.getValue(request.getProtocol(), ""));
      info.setRemoteAddr(StringUtilities.getValue(request.getRemoteAddr(), ""));
      info.setRemoteHost(StringUtilities.getValue(request.getRemoteHost(), ""));
      info.setRemotePort(request.getRemotePort());
      info.setRemoteUser(StringUtilities.getValue(request.getRemoteUser(), ""));
      info.setRequestMethod(StringUtilities.getValue(request.getMethod(), ""));
      info.setScheme(StringUtilities.getValue(request.getScheme(), ""));
      info.setServerName(StringUtilities.getValue(request.getServerName(), ""));
      info.setServerPort(request.getServerPort());
      info.setServletPath(StringUtilities.getValue(request.getServletPath(), ""));
      info.setSessionId(StringUtilities.getValue(request.getRequestedSessionId(), ""));
      
      requestInformation.setInformational(info);
    }
    
    return requestInformation;
  }

  public Headers getHeaders() {
    if (headers == null) {
      headers = OBJECT_FACTORY.createHeaders();
      headers.setRequest(getHeaderList(new RequestHeaderContainer(request)));
      headers.setResponse(getHeaderList(new ResponseHeaderContainer(response)));
    }

    return headers;
  }

  public QueryParameters getRequestParameters() {
    if (queryParameters == null) {
      queryParameters = OBJECT_FACTORY.createQueryParameters();

      if (request != null) {
        List<NameValuePair> parameters = queryParameters.getParameter();
        Set<Entry<String, String[]>> params = request.getParameterMap().entrySet();

        for (Entry<String, String[]> entry : params) {
          for (String value : entry.getValue()) {
            NameValuePair param = new NameValuePair();
            param.setName(entry.getKey());
            param.setValue(value);

            parameters.add(param);
          }
        }

      }
    }

    return queryParameters;
  }
}
