/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.translation.xslt.xmlfilterchain;

import org.apache.commons.pool.ObjectPool;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.filters.translation.config.HttpMethod;
import org.openrepose.filters.translation.xslt.XsltException;
import org.openrepose.filters.translation.xslt.XsltParameter;

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
    private final List<HttpMethod> httpMethods;
    private final List<XsltParameter> params;
    private boolean allMethods;

    public XmlChainPool(String contentType, String accept, List<HttpMethod> httpMethods, String statusRegex, String resultContentType, List<XsltParameter> params, ObjectPool<XmlFilterChain> pool) {
        this.contentType = contentType;
        this.acceptAllContentTypes = StringUtilities.nullSafeEqualsIgnoreCase(this.contentType, MimeType.WILDCARD.getMimeType());
        this.accept = accept;
        this.acceptAll = StringUtilities.nullSafeEqualsIgnoreCase(this.accept, MimeType.WILDCARD.getMimeType());
        this.resultContentType = resultContentType;
        this.objectPool = pool;
        this.httpMethods = httpMethods != null ? httpMethods : new ArrayList<>();
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
        List<XsltParameter<? extends OutputStream>> outputs = new ArrayList<>();
        outputs.add(new XsltParameter<OutputStream>(TranslationResult.HEADERS_OUTPUT, new ByteArrayOutputStream()));
        outputs.add(new XsltParameter<OutputStream>(TranslationResult.QUERY_OUTPUT, new ByteArrayOutputStream()));
        outputs.add(new XsltParameter<OutputStream>(TranslationResult.REQUEST_OUTPUT, new ByteArrayOutputStream()));

        return outputs;
    }

    public TranslationResult executePool(final InputStream in, final OutputStream out, final List<XsltParameter> inputs) {
        TranslationResult rtn = new TranslationResult(false);
        try {
            rtn = doExecutePool(in, out, inputs, objectPool.borrowObject());
        } catch (Exception e) {
            LOG.error("Failed to obtain an XmlFilterChain. Reason: {}", e.getLocalizedMessage());
            LOG.trace("", e);
        }
        return rtn;
    }

    private TranslationResult doExecutePool(final InputStream in, final OutputStream out, final List<XsltParameter> inputs, XmlFilterChain pooledObject) throws Exception {
        TranslationResult rtn = new TranslationResult(false);
        try {
            inputs.addAll(params);
            List<XsltParameter<? extends OutputStream>> outputs = getOutputParameters();
            pooledObject.executeChain(in, out, inputs, outputs);
            rtn = new TranslationResult(true, outputs);
        } catch (XsltException e) {
            objectPool.invalidateObject(pooledObject);
            pooledObject = null;
            LOG.warn("Error processing transforms", e.getMessage(), e);
        } catch (Exception e) {
            objectPool.invalidateObject(pooledObject);
            pooledObject = null;
            LOG.error("Failed to utilize the XmlFilterChain. Reason: {}", e.getLocalizedMessage());
            LOG.trace("", e);
        } finally {
            if (pooledObject != null) {
                objectPool.returnObject(pooledObject);
            }
        }
        return rtn;
    }

    public String getResultContentType() {
        return resultContentType;
    }
}
