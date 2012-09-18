package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.pooling.ConstructionStrategy;
import com.rackspace.papi.commons.util.pooling.GenericBlockingResourcePool;
import com.rackspace.papi.commons.util.pooling.Pool;
import com.rackspace.papi.commons.util.pooling.ResourceConstructionException;
import com.rackspace.papi.components.translation.config.RequestTranslation;
import com.rackspace.papi.components.translation.config.ResponseTranslation;
import com.rackspace.papi.components.translation.config.StyleParam;
import com.rackspace.papi.components.translation.config.StyleSheet;
import com.rackspace.papi.components.translation.config.TranslationConfig;
import com.rackspace.papi.components.translation.xslt.Parameter;
import com.rackspace.papi.components.translation.xslt.StyleSheetInfo;
import com.rackspace.papi.components.translation.xslt.XsltChain;
import com.rackspace.papi.components.translation.xslt.XsltChainBuilder;
import com.rackspace.papi.components.translation.xslt.XsltChainPool;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslationHandlerFactory<T> extends AbstractConfiguredFilterHandlerFactory<TranslationHandler> {

    private TranslationConfig config;
    private final XsltChainBuilder<T> xsltChainBuilder;
    private final ArrayList<XsltChainPool<T>> responseProcessors;
    private final ArrayList<XsltChainPool<T>> requestProcessors;

    public TranslationHandlerFactory(XsltChainBuilder<T> builder) {
        xsltChainBuilder = builder;
        requestProcessors = new ArrayList<XsltChainPool<T>>();
        responseProcessors = new ArrayList<XsltChainPool<T>>();
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
        return new TranslationHandler<T>(config, requestProcessors, responseProcessors);
    }

    class TranslationConfigurationListener implements UpdateListener<TranslationConfig> {

        @Override
        public void configurationUpdated(TranslationConfig config) {
            requestProcessors.clear();
            responseProcessors.clear();

            if (config.getResponseTranslations() != null) {
                for (final ResponseTranslation translation : config.getResponseTranslations().getResponseTranslation()) {

                    final List<Parameter> params = new ArrayList<Parameter>();
                    Pool<XsltChain<T>> pool = new GenericBlockingResourcePool<XsltChain<T>>(
                            new ConstructionStrategy<XsltChain<T>>() {
                                @Override
                                public XsltChain<T> construct() throws ResourceConstructionException {
                                    List<StyleSheetInfo> stylesheets = new ArrayList<StyleSheetInfo>();
                                    for (StyleSheet sheet : translation.getStyleSheets().getStyle()) {
                                        stylesheets.add(new StyleSheetInfo(sheet.getId(), sheet.getHref()));
                                        for (StyleParam param: sheet.getParam()) {
                                            params.add(new Parameter<String>(sheet.getId(), param.getName(), param.getValue()));
                                        }
                                    }

                                    return xsltChainBuilder.build(stylesheets.toArray(new StyleSheetInfo[0]));
                                }
                            });

                    responseProcessors.add(new XsltChainPool(
                            translation.getContentType(),
                            translation.getAccept(),
                            translation.getCodeRegex(),
                            translation.getTranslatedContentType(),
                            params,
                            pool));
                }
            }

            if (config.getRequestTranslations() != null) {
                for (final RequestTranslation translation : config.getRequestTranslations().getRequestTranslation()) {

                    final List<Parameter> params = new ArrayList<Parameter>();
                    Pool<XsltChain<T>> pool = new GenericBlockingResourcePool<XsltChain<T>>(
                            new ConstructionStrategy<XsltChain<T>>() {
                                @Override
                                public XsltChain<T> construct() throws ResourceConstructionException {
                                    List<StyleSheetInfo> stylesheets = new ArrayList<StyleSheetInfo>();
                                    for (StyleSheet sheet : translation.getStyleSheets().getStyle()) {
                                        stylesheets.add(new StyleSheetInfo(sheet.getId(), sheet.getHref()));
                                        for (StyleParam param: sheet.getParam()) {
                                            params.add(new Parameter<String>(sheet.getId(), param.getName(), param.getValue()));
                                        }
                                    }

                                    return xsltChainBuilder.build(stylesheets.toArray(new StyleSheetInfo[0]));
                                }
                            });

                    requestProcessors.add(new XsltChainPool(
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
}
