package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.pooling.Pool;
import com.rackspace.papi.commons.util.pooling.ResourceContext;
import com.rackspace.papi.components.translation.config.HttpMethod;
import com.rackspace.papi.components.translation.xslt.XsltException;
import com.rackspace.papi.components.translation.xslt.XsltParameter;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class XmlChainPool {

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(XmlChainPool.class);
  private final String contentType;
  private final boolean acceptAllContentTypes;
  private final String accept;
  private final boolean acceptAll;
  private final Pool<XmlFilterChain> pool;
  private final String resultContentType;
  private final Pattern statusRegex;
  private boolean allMethods;
  private final List<HttpMethod> httpMethods;
  private final List<XsltParameter> params;

  public XmlChainPool(String contentType, String accept, List<HttpMethod> httpMethods, String statusRegex, String resultContentType, List<XsltParameter> params, Pool<XmlFilterChain> pool) {
    this.contentType = contentType;
    this.acceptAllContentTypes = StringUtilities.nullSafeEqualsIgnoreCase(this.contentType, MimeType.WILDCARD.getMimeType());
    this.accept = accept;
    this.acceptAll = StringUtilities.nullSafeEqualsIgnoreCase(this.accept, MimeType.WILDCARD.getMimeType());
    this.resultContentType = resultContentType;
    this.pool = pool;
    this.httpMethods = httpMethods != null ? httpMethods : new ArrayList<HttpMethod>();
    this.statusRegex = StringUtilities.isNotBlank(statusRegex) ? Pattern.compile(statusRegex) : null;
    this.params = params;
    if (this.httpMethods.isEmpty()) {
      this.allMethods = true;
    } else {
      for (HttpMethod method : this.httpMethods) {
        this.allMethods |= "ALL".equalsIgnoreCase(method.name());
      }
    }
  }

  private boolean matchesMethod(String requestMethod) {
    boolean result = false;
    for (HttpMethod method : httpMethods) {
      result |= method.name().equalsIgnoreCase(requestMethod);
    }

    return result;
  }

  public boolean accepts(String method, MediaType contentType, MediaType accept, String statusCode) {
    boolean matchesAccept = acceptAll || StringUtilities.nullSafeEqualsIgnoreCase(this.accept, accept.getValue());
    boolean matchesContentType = acceptAllContentTypes || StringUtilities.nullSafeEqualsIgnoreCase(this.contentType, contentType.getValue());
    boolean matchesStatusCode = statusRegex != null && StringUtilities.isNotBlank(statusCode) ? statusRegex.matcher(statusCode).matches() : true;
    boolean matchesMethod = StringUtilities.isNotBlank(method) ? allMethods || matchesMethod(method) : true;

    return matchesAccept && matchesContentType && matchesStatusCode && matchesMethod;
  }

  private List<XsltParameter<? extends OutputStream>> getOutputParameters() {
    List<XsltParameter<? extends OutputStream>> outputs = new ArrayList<XsltParameter<? extends OutputStream>>();
    outputs.add(new XsltParameter<OutputStream>(TranslationResult.HEADERS_OUTPUT, new ByteArrayOutputStream()));
    outputs.add(new XsltParameter<OutputStream>(TranslationResult.QUERY_OUTPUT, new ByteArrayOutputStream()));
    outputs.add(new XsltParameter<OutputStream>(TranslationResult.REQUEST_OUTPUT, new ByteArrayOutputStream()));

    return outputs;
  }

  public TranslationResult executePool(final InputStream in, final OutputStream out, final List<XsltParameter> inputs) {
    TranslationResult result = (TranslationResult) getPool().use(new ResourceContext<XmlFilterChain, TranslationResult>() {
      @Override
      public TranslationResult perform(XmlFilterChain chain) {

        inputs.addAll(getParams());
        List<XsltParameter<? extends OutputStream>> outputs = getOutputParameters();

        try {
          chain.executeChain(in, out, inputs, outputs);
        } catch (XsltException ex) {
          LOG.warn("Error processing transforms", ex.getMessage());
          return new TranslationResult(false);
        }
        return new TranslationResult(true, outputs);
      }
    });

    return result;

  }

  public String getContentType() {
    return contentType;
  }

  public String getAccept() {
    return accept;
  }

  public Pool<XmlFilterChain> getPool() {
    return pool;
  }

  public String getResultContentType() {
    return resultContentType;
  }

  public Pattern getStatusRegex() {
    return statusRegex;
  }

  public List<XsltParameter> getParams() {
    return params;
  }
}
