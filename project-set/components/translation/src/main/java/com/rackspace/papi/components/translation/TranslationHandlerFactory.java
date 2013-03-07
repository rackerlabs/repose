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
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;

public class TranslationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<TranslationHandler> {

  private final SAXTransformerFactory transformerFactory;
  private final List<XmlChainPool> responseProcessorPools;
  private final List<XmlChainPool> requestProcessorPools;
  private final String configurationRoot;
  private final Object lock = new Object();
  private final XslUpdateListener xslListener;
  private final String config;
  private TranslationConfig configuration;
  private XmlFilterChainBuilder xsltChainBuilder;

  public TranslationHandlerFactory(ConfigurationService configService, String configurationRoot, String config) {
    transformerFactory = (SAXTransformerFactory) TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
    requestProcessorPools = new ArrayList<XmlChainPool>();
    responseProcessorPools = new ArrayList<XmlChainPool>();
    this.configurationRoot = configurationRoot;
    this.config = config;
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

    if (!this.isInitialized()) {
      return null;
    }
    synchronized (lock) {
      return new TranslationHandler(new ArrayList<XmlChainPool>(requestProcessorPools), new ArrayList<XmlChainPool>(responseProcessorPools));
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
    return new GenericBlockingResourcePool<XmlFilterChain>(new XmlFilterChainFactory(xsltChainBuilder, translation, configurationRoot, config));
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
          Pool<XmlFilterChain> pool = buildChainPool(translation);

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

    private boolean isInitialized = false;

    @Override
    public void configurationUpdated(TranslationConfig newConfig) {
      synchronized (lock) {
        configuration = newConfig;
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
  }
}
