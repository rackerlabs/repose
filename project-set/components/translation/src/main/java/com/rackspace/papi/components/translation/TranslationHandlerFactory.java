package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.pooling.ConstructionStrategy;
import com.rackspace.papi.commons.util.pooling.GenericBlockingResourcePool;
import com.rackspace.papi.commons.util.pooling.Pool;
import com.rackspace.papi.commons.util.pooling.ResourceConstructionException;
import com.rackspace.papi.components.translation.config.ResponseTranslation;
import com.rackspace.papi.components.translation.config.StyleSheet;
import com.rackspace.papi.components.translation.config.TranslationConfig;
import com.rackspace.papi.components.translation.xslt.handlerchain.StyleSheetInfo;
import com.rackspace.papi.components.translation.xslt.handlerchain.XsltHandlerChain;
import com.rackspace.papi.components.translation.xslt.handlerchain.XsltHandlerChainBuilder;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;

public class TranslationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<TranslationHandler> {

    private TranslationConfig config;
    private final SAXTransformerFactory transformerFactory;
    private final XsltHandlerChainBuilder xsltChainBuilder;
    private final ArrayList<XsltHandlerChainPool> responseProcessors;
    private final ArrayList<XsltHandlerChainPool> requestProcessors;

    public TranslationHandlerFactory() {
        transformerFactory = (SAXTransformerFactory) TransformerFactory.newInstance();
        xsltChainBuilder = new XsltHandlerChainBuilder(transformerFactory);
        requestProcessors = new ArrayList<XsltHandlerChainPool>();
        responseProcessors = new ArrayList<XsltHandlerChainPool>();
        
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
        return new TranslationHandler(config, requestProcessors, responseProcessors);
    }
    
    private class TranslationConfigurationListener implements UpdateListener<TranslationConfig> {

        @Override
        public void configurationUpdated(TranslationConfig config) {
            requestProcessors.clear();
            responseProcessors.clear();

            for (final ResponseTranslation translation : config.getResponseTranslations().getResponseTranslation()) {

                Pool<XsltHandlerChain> pool = new GenericBlockingResourcePool<XsltHandlerChain>(
                        new ConstructionStrategy<XsltHandlerChain>() {
                            @Override
                            public XsltHandlerChain construct() throws ResourceConstructionException {
                                List<StyleSheetInfo> stylesheets = new ArrayList<StyleSheetInfo>();
                                for (StyleSheet sheet : translation.getStyleSheets().getStyle()) {
                                    stylesheets.add(new StyleSheetInfo(sheet.getId(), sheet.getHref()));
                                }

                                return xsltChainBuilder.build(stylesheets.toArray(new StyleSheetInfo[0]));
                            }
                        });
                
                responseProcessors.add(new XsltHandlerChainPool(translation.getContentType(), translation.getAccept(), translation.getTranslatedContentType(), pool));
            }

        }
    }
}
