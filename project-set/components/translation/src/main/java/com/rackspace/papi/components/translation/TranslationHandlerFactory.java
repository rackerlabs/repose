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
import com.rackspace.papi.components.translation.xslt.XsltParameter;
import com.rackspace.papi.components.translation.xslt.xmlfilterchain.XmlChainPool;
import com.rackspace.papi.components.translation.xslt.xmlfilterchain.XmlFilterChain;
import com.rackspace.papi.components.translation.xslt.xmlfilterchain.XmlFilterChainBuilder;
import com.rackspace.papi.components.translation.xslt.xmlfilterchain.XmlFilterChainFactory;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.service.config.ConfigurationService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<TranslationHandler> {

    private TranslationConfig configuration;
    private final XmlFilterChainBuilder xsltChainBuilder;
    private final List<XmlChainPool> responseProcessorPools;
    private final List<XmlChainPool> requestProcessorPools;
    private final String configurationRoot;
    private final Object lock = new Object();
    private final XslUpdateListener xslListener;

    public TranslationHandlerFactory(ConfigurationService configService, XmlFilterChainBuilder builder, String configurationRoot) {
        xsltChainBuilder = builder;
        requestProcessorPools = new ArrayList<XmlChainPool>();
        responseProcessorPools = new ArrayList<XmlChainPool>();
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
            return new TranslationHandler(configuration, new ArrayList<XmlChainPool>(requestProcessorPools), new ArrayList<XmlChainPool>(responseProcessorPools));
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

    private Pool<XmlFilterChain> buildChainPool(final TranslationBase translation) {
        return new GenericBlockingResourcePool<XmlFilterChain>(new XmlFilterChainFactory(xsltChainBuilder, translation, configurationRoot));
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
                    Pool<XmlFilterChain> pool = buildChainPool(translation);
                    addStyleSheetsToWatchList(translation);

                    responseProcessorPools.add(new XmlChainPool(
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
                    Pool<XmlFilterChain> pool = buildChainPool(translation);
                    addStyleSheetsToWatchList(translation);

                    requestProcessorPools.add(new XmlChainPool(
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
