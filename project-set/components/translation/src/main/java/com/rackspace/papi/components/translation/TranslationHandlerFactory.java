package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.parser.generic.GenericResourceConfigurationParser;
import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.pooling.ConstructionStrategy;
import com.rackspace.papi.commons.util.pooling.GenericBlockingResourcePool;
import com.rackspace.papi.commons.util.pooling.Pool;
import com.rackspace.papi.commons.util.pooling.ResourceConstructionException;
import com.rackspace.papi.components.translation.config.RequestTranslation;
import com.rackspace.papi.components.translation.config.ResponseTranslation;
import com.rackspace.papi.components.translation.config.StyleParam;
import com.rackspace.papi.components.translation.config.StyleSheet;
import com.rackspace.papi.components.translation.config.TranslationBase;
import com.rackspace.papi.components.translation.config.TranslationConfig;
import com.rackspace.papi.components.translation.xslt.Parameter;
import com.rackspace.papi.components.translation.xslt.StyleSheetInfo;
import com.rackspace.papi.components.translation.xslt.XsltChain;
import com.rackspace.papi.components.translation.xslt.XsltChainBuilder;
import com.rackspace.papi.components.translation.xslt.XsltChainPool;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.service.config.ConfigurationService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class TranslationHandlerFactory<T> extends AbstractConfiguredFilterHandlerFactory<TranslationHandler> {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TranslationHandlerFactory.class);
    private TranslationConfig configuration;
    private final XsltChainBuilder<T> xsltChainBuilder;
    private final ArrayList<XsltChainPool<T>> responseProcessorPools;
    private final ArrayList<XsltChainPool<T>> requestProcessorPools;
    private final String configurationRoot;
    private final Object lock = new Object();
    private final XslListener xslListener;
    private final ConfigurationService configurationService;
    private final HashSet<String> xslWatchList;

    public TranslationHandlerFactory(ConfigurationService manager, XsltChainBuilder<T> builder, String configurationRoot) {
        xsltChainBuilder = builder;
        requestProcessorPools = new ArrayList<XsltChainPool<T>>();
        responseProcessorPools = new ArrayList<XsltChainPool<T>>();
        this.configurationRoot = configurationRoot;
        xslListener = new XslListener();
        this.configurationService = manager;
        xslWatchList = new HashSet<String>();
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {
            {
                put(TranslationConfig.class, new TranslationConfigurationListener());
            }
        };
    }

    @Override
    protected TranslationHandler buildHandler() {
        synchronized (lock) {
            return new TranslationHandler<T>(configuration, new ArrayList<XsltChainPool<T>>(requestProcessorPools), new ArrayList<XsltChainPool<T>>(responseProcessorPools));
        }
    }

    String getAbsoluteXslPath(String xslPath) {
        return !xslPath.contains("://") ? StringUtilities.join("file://", configurationRoot, "/", xslPath) : xslPath;
    }

    class XslListener implements UpdateListener<ConfigurationResource> {

        @Override
        public void configurationUpdated(ConfigurationResource config) {
            LOG.info("XSL file changed: " + config.name());

            updateProcessors();
        }
    }

    private void unsubscribeAllXslListeners() {
        synchronized (lock) {
            for (String xsl : xslWatchList) {
                configurationService.unsubscribeFrom(xsl, xslListener);
            }
        }
    }

    private void addXslListener(String xsl) {
        LOG.info("Watching XSL: " + xsl);
        configurationService.subscribeTo(xsl, xslListener, new GenericResourceConfigurationParser(), false);
    }

    private List<Parameter> buildXslParamList(TranslationBase translation) {
        final List<Parameter> params = new ArrayList<Parameter>();
        if (translation.getStyleSheets() != null) {
            for (StyleSheet sheet : translation.getStyleSheets().getStyle()) {
                for (StyleParam param : sheet.getParam()) {
                    params.add(new Parameter<String>(sheet.getId(), param.getName(), param.getValue()));
                }
            }
        }
        return params;
    }

    private Pool<XsltChain<T>> buildChainPool(final TranslationBase translation) {
        Pool<XsltChain<T>> pool = new GenericBlockingResourcePool<XsltChain<T>>(
                new ConstructionStrategy<XsltChain<T>>() {
                    @Override
                    public XsltChain<T> construct() throws ResourceConstructionException {
                        List<StyleSheetInfo> stylesheets = new ArrayList<StyleSheetInfo>();
                        if (translation.getStyleSheets() != null) {
                            for (StyleSheet sheet : translation.getStyleSheets().getStyle()) {
                                stylesheets.add(new StyleSheetInfo(sheet.getId(), getAbsoluteXslPath(sheet.getHref())));
                            }
                        }

                        return xsltChainBuilder.build(stylesheets.toArray(new StyleSheetInfo[0]));
                    }
                });

        return pool;
    }

    private void addXslsToList(final TranslationBase translation) {
        if (translation.getStyleSheets() != null) {
            for (StyleSheet sheet : translation.getStyleSheets().getStyle()) {
                xslWatchList.add(getAbsoluteXslPath(sheet.getHref()));
            }
        }
    }

    private void updateProcessors() {
        synchronized (lock) {
            requestProcessorPools.clear();
            responseProcessorPools.clear();

            if (configuration.getResponseTranslations() != null) {
                for (final ResponseTranslation translation : configuration.getResponseTranslations().getResponseTranslation()) {

                    List<Parameter> params = buildXslParamList(translation);
                    Pool<XsltChain<T>> pool = buildChainPool(translation);
                    addXslsToList(translation);

                    responseProcessorPools.add(new XsltChainPool(
                            translation.getContentType(),
                            translation.getAccept(),
                            translation.getCodeRegex(),
                            translation.getTranslatedContentType(),
                            params,
                            pool));
                }
            }

            if (configuration.getRequestTranslations() != null) {
                for (final RequestTranslation translation : configuration.getRequestTranslations().getRequestTranslation()) {

                    List<Parameter> params = buildXslParamList(translation);
                    Pool<XsltChain<T>> pool = buildChainPool(translation);
                    addXslsToList(translation);

                    requestProcessorPools.add(new XsltChainPool(
                            translation.getContentType(),
                            translation.getAccept(),
                            translation.getHttpMethods(),
                            translation.getTranslatedContentType(),
                            params,
                            pool));
                }
            }

        }
    }

    class TranslationConfigurationListener implements UpdateListener<TranslationConfig> {

        @Override
        public void configurationUpdated(TranslationConfig config) {
            configuration = config;
            unsubscribeAllXslListeners();
            xslWatchList.clear();
            updateProcessors();
            for (String xsl : xslWatchList) {
                addXslListener(xsl);
            }
        }
    }
}
