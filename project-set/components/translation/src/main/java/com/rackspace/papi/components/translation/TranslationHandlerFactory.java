package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.pooling.GenericBlockingResourcePool;
import com.rackspace.papi.commons.util.pooling.Pool;
import com.rackspace.papi.components.translation.config.RequestTranslation;
import com.rackspace.papi.components.translation.config.ResponseTranslation;
import com.rackspace.papi.components.translation.config.StyleParam;
import com.rackspace.papi.components.translation.config.StyleSheet;
import com.rackspace.papi.components.translation.config.TranslationBase;
import com.rackspace.papi.components.translation.config.TranslationConfig;
import com.rackspace.papi.components.translation.xslt.XsltChain;
import com.rackspace.papi.components.translation.xslt.XsltChainBuilder;
import com.rackspace.papi.components.translation.xslt.XsltChainFactory;
import com.rackspace.papi.components.translation.xslt.XsltChainPool;
import com.rackspace.papi.components.translation.xslt.XsltParameter;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.service.config.ConfigurationService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslationHandlerFactory<T> extends AbstractConfiguredFilterHandlerFactory<TranslationHandler> {

    private TranslationConfig configuration;
    private final XsltChainBuilder<T> xsltChainBuilder;
    private final List<XsltChainPool<T>> responseProcessorPools;
    private final List<XsltChainPool<T>> requestProcessorPools;
    private final String configurationRoot;
    private final Object lock = new Object();
    private final XslUpdateListener xslListener;

    public TranslationHandlerFactory(ConfigurationService configService, XsltChainBuilder<T> builder, String configurationRoot) {
        xsltChainBuilder = builder;
        requestProcessorPools = new ArrayList<XsltChainPool<T>>();
        responseProcessorPools = new ArrayList<XsltChainPool<T>>();
        this.configurationRoot = configurationRoot;
        xslListener = new XslUpdateListener(this, configService, configurationRoot);
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

    private List<XsltParameter> buildXslParamList(TranslationBase translation) {
        final List<XsltParameter> params = new ArrayList<XsltParameter>();
        if (translation.getStyleSheets() != null) {
            for (StyleSheet sheet : translation.getStyleSheets().getStyle()) {
                for (StyleParam param : sheet.getParam()) {
                    params.add(new XsltParameter<String>(sheet.getId(), param.getName(), param.getValue()));
                }
            }
        }
        return params;
    }

    private Pool<XsltChain<T>> buildChainPool(final TranslationBase translation) {
        return new GenericBlockingResourcePool<XsltChain<T>>(new XsltChainFactory<T>(xsltChainBuilder, translation, configurationRoot));
    }

    private void addStyleSheetsToWatchList(final TranslationBase translation) {
        if (translation.getStyleSheets() != null) {
            for (StyleSheet sheet : translation.getStyleSheets().getStyle()) {
                xslListener.addToWatchList(sheet.getHref());
            }
        }
    }

    public void buildProcessorPools() {
        synchronized (lock) {
            requestProcessorPools.clear();
            responseProcessorPools.clear();

            if (configuration.getResponseTranslations() != null) {
                for (final ResponseTranslation translation : configuration.getResponseTranslations().getResponseTranslation()) {

                    List<XsltParameter> params = buildXslParamList(translation);
                    Pool<XsltChain<T>> pool = buildChainPool(translation);
                    addStyleSheetsToWatchList(translation);

                    responseProcessorPools.add(new XsltChainPool(
                            translation.getContentType(),
                            translation.getAccept(),
                            null,
                            translation.getCodeRegex(),
                            translation.getTranslatedContentType(),
                            params,
                            pool));
                }
            }

            if (configuration.getRequestTranslations() != null) {
                for (final RequestTranslation translation : configuration.getRequestTranslations().getRequestTranslation()) {

                    List<XsltParameter> params = buildXslParamList(translation);
                    Pool<XsltChain<T>> pool = buildChainPool(translation);
                    addStyleSheetsToWatchList(translation);

                    requestProcessorPools.add(new XsltChainPool(
                            translation.getContentType(),
                            translation.getAccept(),
                            translation.getHttpMethods(),
                            null,
                            translation.getTranslatedContentType(),
                            params,
                            pool));
                }
            }

        }
    }

    class TranslationConfigurationListener implements UpdateListener<TranslationConfig> {

        @Override
        public void configurationUpdated(TranslationConfig newConfig) {
            synchronized (lock) {
                configuration = newConfig;
                xslListener.unsubscribe();
                buildProcessorPools();
                xslListener.listen();
            }
        }
    }
}
