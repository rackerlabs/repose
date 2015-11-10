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
package org.openrepose.filters.translation;

import com.saxonica.config.EnterpriseTransformerFactory;
import net.sf.saxon.TransformerFactoryImpl;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.io.ByteBufferInputStream;
import org.openrepose.commons.utils.io.ByteBufferServletOutputStream;
import org.openrepose.commons.utils.io.buffer.ByteBuffer;
import org.openrepose.commons.utils.io.buffer.CyclicByteBuffer;
import org.openrepose.commons.utils.servlet.filter.FilterAction;
import org.openrepose.commons.utils.servlet.http.HandleRequestResult;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.commons.utils.servlet.http.HttpServletResponseWrapper;
import org.openrepose.commons.utils.servlet.http.HttpServletWrappersHelper;
import org.openrepose.core.filter.FilterConfigHelper;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.openrepose.filters.translation.config.*;
import org.openrepose.filters.translation.httpx.processor.TranslationPreProcessor;
import org.openrepose.filters.translation.xslt.XsltParameter;
import org.openrepose.filters.translation.xslt.xmlfilterchain.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.http.HttpHeaders.CONTENT_LENGTH;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.openrepose.commons.utils.servlet.filter.FilterAction.PROCESS_RESPONSE;
import static org.openrepose.commons.utils.servlet.filter.FilterAction.RETURN;
import static org.openrepose.commons.utils.servlet.http.ResponseMode.MUTABLE;

@Named
public class TranslationFilter implements Filter, UpdateListener<TranslationConfig> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(TranslationFilter.class);
    private static final String DEFAULT_CONFIG = "translation.cfg.xml";

    public static final String SAXON_HE_FACTORY_NAME = "net.sf.saxon.TransformerFactoryImpl";
    public static final String SAXON_EE_FACTORY_NAME = "com.saxonica.config.EnterpriseTransformerFactory";
    public static final String INPUT_HEADERS_URI = "input-headers-uri";
    public static final String INPUT_QUERY_URI = "input-query-uri";
    public static final String INPUT_REQUEST_URI = "input-request-uri";
    private static final int DEFAULT_BUFFER_SIZE = 2048;

    private final ConfigurationService configurationService;
    private final String configurationRoot;
    private final Object lock = new Object();
    private String config;
    private boolean isInitialized = false;

    private List<XmlChainPool> requestProcessorPools;
    private List<XmlChainPool> responseProcessorPools;
    private XslUpdateListener xslListener;
    private SAXTransformerFactory transformerFactory;
    private TranslationConfig configuration;
    private XmlFilterChainBuilder xsltChainBuilder;

    @Inject
    public TranslationFilter(ConfigurationService configurationService,
                             @Value(ReposeSpringProperties.CORE.CONFIG_ROOT) String configurationRoot) {
        this.configurationService = configurationService;
        this.configurationRoot = configurationRoot;
        transformerFactory = (SAXTransformerFactory) TransformerFactory.newInstance(SAXON_HE_FACTORY_NAME, this.getClass().getClassLoader());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        requestProcessorPools = new ArrayList<>();
        responseProcessorPools = new ArrayList<>();
        xslListener = new XslUpdateListener(this, configurationService, configurationRoot);
        URL xsdURL = getClass().getResource("/META-INF/schema/config/translation-configuration.xsd");
        configurationService.subscribeTo(
                filterConfig.getFilterName(),
                config,
                xsdURL,
                this,
                TranslationConfig.class
        );
    }

    @Override
    public void configurationUpdated(TranslationConfig newConfig) {
        synchronized (lock) {
            configuration = newConfig;

            if (configuration.getXslEngine() == XSLEngine.SAXON_EE) {
                updateTransformerPool(SAXON_EE_FACTORY_NAME);
                /*
                 * I found this through here: http://sourceforge.net/p/saxon/mailman/message/29737564/
                 * A bit of deduction and stuff let me to assume that all dynamic loading is done with the DynamicLoader
                 * object. The only way to get a hold of that is to typecast the TransformerFactory to the actual class,
                 * get the DynamicLoader out of it, and set it's classloader to the one where the saxonica classes are located.
                 */
                //Now that we have a Saxon EE transformer factory, we need to configure it...
                //We have to do casting to get the configuration object, to configure the DynamicLoader for our classloader
                //This is only needed for saxon EE, because it generates bytecode.
                EnterpriseTransformerFactory etf = (EnterpriseTransformerFactory) transformerFactory;
                etf.getConfiguration().getDynamicLoader().setClassLoader(this.getClass().getClassLoader());
            } else {
                updateTransformerPool(SAXON_HE_FACTORY_NAME);
                TransformerFactoryImpl tfi = (TransformerFactoryImpl) transformerFactory;
                tfi.getConfiguration().getDynamicLoader().setClassLoader(this.getClass().getClassLoader());
            }

            xslListener.unsubscribe();
            try {
                xsltChainBuilder = new XmlFilterChainBuilder(transformerFactory, false, configuration.isAllowDoctypeDecl());
                buildProcessorPools();
            } finally {
                xslListener.listen();
            }
        }
        isInitialized = true;
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper((HttpServletRequest) request);
        HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(
                (HttpServletResponse) response,
                MUTABLE,
                MUTABLE,
                response.getOutputStream()
        );

        final HandleRequestResult handleRequestResult = handleRequest(requestWrapper, responseWrapper);
        HttpServletRequestWrapper handleRequestWrapper = handleRequestResult.getRequest();
        HttpServletResponseWrapper handleResponseWrapper = handleRequestResult.getResponse();

        switch (handleRequestResult.getFilterAction()) {
            case NOT_SET:
                chain.doFilter(request, response);
                break;
            case PASS:
                chain.doFilter(handleRequestWrapper, handleResponseWrapper);
                handleResponseWrapper.commitToResponse();
                break;
            case PROCESS_RESPONSE:
                chain.doFilter(handleRequestWrapper, handleResponseWrapper);
                handleResponse(handleRequestWrapper, handleResponseWrapper);
                handleResponseWrapper.commitToResponse();
                break;
            case RETURN:
                break;
        }
    }

    private List<XmlChainPool> getHandlerChainPool(String method, MediaType contentType, List<MediaType> accept, String status, List<XmlChainPool> pools) {
        List<XmlChainPool> chains = new ArrayList<>();

        for (MediaType value : accept) {
            for (XmlChainPool pool : pools) {
                if (pool.accepts(method, contentType, value, status)) {
                    chains.add(pool);
                    if (!configuration.isMultiMatch()) {
                        break;
                    }
                }
            }
        }

        return chains;
    }

    private List<XsltParameter> getInputParameters(final TranslationType type, final HttpServletRequestWrapper request, final HttpServletResponseWrapper response, final TranslationResult lastResult) {
        List<XsltParameter> inputs = new ArrayList<>();
        final String requestId = (String) request.getAttribute("requestId");
        inputs.add(new XsltParameter<>("request", request));
        inputs.add(new XsltParameter<>("response", response));
        inputs.add(new XsltParameter<>("requestId", requestId));
        if (lastResult != null) {
            if (lastResult.getRequestInfo() != null) {
                inputs.add(new XsltParameter<>("requestInfo", lastResult.getRequestInfo()));
            }
            if (lastResult.getHeaders() != null) {
                inputs.add(new XsltParameter<>("headers", lastResult.getHeaders()));
            }
            if (lastResult.getQueryParameters() != null) {
                inputs.add(new XsltParameter<>("queryParams", lastResult.getQueryParameters()));
            }
        }

        /* Input/Output URIs */
        inputs.add(new XsltParameter<>(INPUT_HEADERS_URI, "repose:input:headers:" + requestId));
        inputs.add(new XsltParameter<>(INPUT_QUERY_URI, "repose:input:query:" + requestId));
        inputs.add(new XsltParameter<>(INPUT_REQUEST_URI, "repose:input:request:" + requestId));
        inputs.add(new XsltParameter<>("output-headers-uri", "repose:output:headers.xml"));
        inputs.add(new XsltParameter<>("output-query-uri", "repose:output:query.xml"));
        inputs.add(new XsltParameter<>("output-request-uri", "repose:output:request.xml"));

        return inputs;
    }

    private enum TranslationType {
        REQUEST,
        RESPONSE
    }

    public HandleRequestResult handleRequest(HttpServletRequestWrapper request, HttpServletResponseWrapper response) {
        FilterAction filterAction = RETURN;
        HttpServletRequestWrapper rtnRequest = request;
        MediaType contentType = HttpServletWrappersHelper.getContentType(rtnRequest);
        List<MediaType> acceptValues = HttpServletWrappersHelper.getAcceptValues(rtnRequest);
        List<XmlChainPool> pools = getHandlerChainPool(
                rtnRequest.getMethod(),
                contentType,
                acceptValues,
                "",
                new ArrayList<>(requestProcessorPools)
        );

        if (pools.isEmpty()) {
            filterAction = PROCESS_RESPONSE;
        } else {
            try {
                ServletInputStream in = rtnRequest.getInputStream();
                TranslationResult result = null;
                for (XmlChainPool pool : pools) {
                    final ByteBuffer internalBuffer = new CyclicByteBuffer(DEFAULT_BUFFER_SIZE, true);
                    result = pool.executePool(
                            new TranslationPreProcessor(in, contentType, true).getBodyStream(),
                            new ByteBufferServletOutputStream(internalBuffer),
                            getInputParameters(TranslationType.REQUEST, rtnRequest, response, result)
                    );

                    if (result.isSuccess()) {
                        rtnRequest = new HttpServletRequestWrapper(rtnRequest, new ByteBufferInputStream(internalBuffer));
                        if (StringUtilities.isNotBlank(pool.getResultContentType())) {
                            rtnRequest.replaceHeader(CONTENT_TYPE, pool.getResultContentType());
                            contentType = HttpServletWrappersHelper.getContentType(pool.getResultContentType());
                        }
                        response.setStatus(SC_OK);
                        filterAction = PROCESS_RESPONSE;
                    } else {
                        response.setStatus(SC_BAD_REQUEST);
                        break;
                    }
                }
            } catch (IOException ex) {
                LOG.error("Error executing request transformer chain", ex);
                response.setStatus(SC_INTERNAL_SERVER_ERROR);
            }
        }

        return new HandleRequestResult(filterAction, rtnRequest, response);
    }

    public void handleResponse(HttpServletRequestWrapper request, HttpServletResponseWrapper response) {
        MediaType contentType = HttpServletWrappersHelper.getContentType(response);
        List<MediaType> acceptValues = HttpServletWrappersHelper.getAcceptValues(request);
        List<XmlChainPool> pools = getHandlerChainPool(
                "",
                contentType,
                acceptValues,
                String.valueOf(response.getStatus()),
                new ArrayList<>(responseProcessorPools)
        );

        if (!pools.isEmpty()) {
            try {
                InputStream in = response.getOutputStreamAsInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                if (in != null) {

                    TranslationResult result = null;
                    for (XmlChainPool pool : pools) {
                        if (in.available() > 0) {
                            result = pool.executePool(
                                    new TranslationPreProcessor(in, contentType, true).getBodyStream(),
                                    baos,
                                    getInputParameters(TranslationType.RESPONSE, request, response, result)
                            );

                            if (result.isSuccess()) {
                                if (StringUtilities.isNotBlank(pool.getResultContentType())) {
                                    request.replaceHeader(CONTENT_TYPE, pool.getResultContentType());
                                    contentType = HttpServletWrappersHelper.getContentType(pool.getResultContentType());
                                }
                                response.setOutput(new ByteArrayInputStream(baos.toByteArray()));
                            } else {
                                response.setStatus(SC_INTERNAL_SERVER_ERROR);
                                response.setContentLength(0);
                                response.removeHeader(CONTENT_LENGTH);
                                break;
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                LOG.error("Error executing response transformer chain", ex);
                response.setStatus(SC_INTERNAL_SERVER_ERROR);
                response.setContentLength(0);
            }
        }
    }

    @Override
    public void destroy() {
        configurationService.unsubscribeFrom(config, this);
    }

    private List<XsltParameter> buildXslParamList(TranslationBase translation) {
        final List<XsltParameter> params = new ArrayList<>();
        if (translation.getStyleSheets() != null) {
            for (StyleSheet sheet : translation.getStyleSheets().getStyle()) {
                for (StyleParam param : sheet.getParam()) {
                    params.add(new XsltParameter<>(sheet.getId(), param.getName(), param.getValue()));
                }
            }
        }
        return params;
    }

    private ObjectPool<XmlFilterChain> buildChainPool(final TranslationBase translation) {
        return new SoftReferenceObjectPool<>(new XmlFilterChainFactory(xsltChainBuilder, translation, configurationRoot, config));
    }

    private void addStyleSheetsToWatchList(final TranslationBase translation) {
        if (translation.getStyleSheets() != null) {
            for (StyleSheet sheet : translation.getStyleSheets().getStyle()) {
                if (sheet.getHref() != null) {
                    xslListener.addToWatchList(sheet.getHref());
                }
            }
        }
    }

    public void buildProcessorPools() {
        synchronized (lock) {
            requestProcessorPools.clear();
            responseProcessorPools.clear();

            if (configuration.getResponseTranslations() != null) {
                for (final ResponseTranslation translation : configuration.getResponseTranslations().getResponseTranslation()) {
                    addStyleSheetsToWatchList(translation);
                }
            }

            if (configuration.getRequestTranslations() != null) {
                for (final RequestTranslation translation : configuration.getRequestTranslations().getRequestTranslation()) {
                    addStyleSheetsToWatchList(translation);
                }
            }

            if (configuration.getResponseTranslations() != null) {
                for (final ResponseTranslation translation : configuration.getResponseTranslations().getResponseTranslation()) {
                    List<XsltParameter> params = buildXslParamList(translation);
                    ObjectPool<XmlFilterChain> pool = buildChainPool(translation);

                    responseProcessorPools.add(new XmlChainPool(
                            translation.getContentType(),
                            translation.getAccept(),
                            null,
                            translation.getCodeRegex(),
                            translation.getTranslatedContentType(),
                            params,
                            pool
                    ));
                }
            }

            if (configuration.getRequestTranslations() != null) {
                for (final RequestTranslation translation : configuration.getRequestTranslations().getRequestTranslation()) {

                    List<XsltParameter> params = buildXslParamList(translation);
                    ObjectPool<XmlFilterChain> pool = buildChainPool(translation);

                    requestProcessorPools.add(new XmlChainPool(
                            translation.getContentType(),
                            translation.getAccept(),
                            translation.getHttpMethods(),
                            null,
                            translation.getTranslatedContentType(),
                            params,
                            pool
                    ));
                }
            }
        }
    }

    private void updateTransformerPool(String transFactoryClass) {
        if (!transformerFactory.getClass().getCanonicalName().equals(transFactoryClass)) {
            transformerFactory = (SAXTransformerFactory) TransformerFactory.newInstance(transFactoryClass, this.getClass().getClassLoader());
        }
    }
}
