package org.openrepose.filters.translation.xslt.xmlfilterchain;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.filters.translation.config.HttpMethod;
import org.openrepose.filters.translation.xslt.XsltException;
import org.openrepose.filters.translation.xslt.XsltParameter;
import org.apache.commons.pool.ObjectPool;

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
  private final ObjectPool<XmlFilterChain> objectPool;
  private final String resultContentType;
  private final Pattern statusRegex;
  private boolean allMethods;
  private final List<HttpMethod> httpMethods;
  private final List<XsltParameter> params;

  public XmlChainPool(String contentType, String accept, List<HttpMethod> httpMethods, String statusRegex, String resultContentType, List<XsltParameter> params, ObjectPool<XmlFilterChain> pool) {
    this.contentType = contentType;
    this.acceptAllContentTypes = StringUtilities.nullSafeEqualsIgnoreCase(this.contentType, MimeType.WILDCARD.getMimeType());
    this.accept = accept;
    this.acceptAll = StringUtilities.nullSafeEqualsIgnoreCase(this.accept, MimeType.WILDCARD.getMimeType());
    this.resultContentType = resultContentType;
    this.objectPool = pool;
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
        TranslationResult rtn = null;
        XmlFilterChain pooledObject = null;
        try {
            pooledObject = objectPool.borrowObject();
            try {
                inputs.addAll(params);
                List<XsltParameter<? extends OutputStream>> outputs = getOutputParameters();
                pooledObject.executeChain(in, out, inputs, outputs);
                rtn = new TranslationResult(true, outputs);
            } catch (XsltException e) {
                LOG.warn("Error processing transforms", e.getMessage(), e);
                rtn = new TranslationResult(false);
            } catch (Exception e) {
                objectPool.invalidateObject(pooledObject);
                pooledObject = null;
                LOG.error("Failed to utilize the XmlFilterChain.", e);
            } finally {
                if (null != pooledObject) {
                    objectPool.returnObject(pooledObject);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to obtain a FIX_POOLED", e);
        }

        return rtn;
  }

  public String getResultContentType() {
    return resultContentType;
  }
}
