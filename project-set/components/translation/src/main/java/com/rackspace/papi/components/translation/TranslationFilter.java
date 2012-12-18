package com.rackspace.papi.components.translation;

import com.rackspace.papi.components.translation.config.TranslationConfig;
import com.rackspace.papi.components.translation.xslt.xmlfilterchain.XmlFilterChainBuilder;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.servlet.InitParameter;
import java.io.IOException;
import java.net.URL;
import javax.servlet.*;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import org.slf4j.Logger;

public class TranslationFilter implements Filter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(TranslationFilter.class);
    private static final String DEFAULT_CONFIG = "translation.cfg.xml";
    private String config;
    private TranslationHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom(config, handlerFactory);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        final ServletContext servletContext = filterConfig.getServletContext();
        final String configProp = InitParameter.POWER_API_CONFIG_DIR.getParameterName();
        final ServletContext ctx = filterConfig.getServletContext();
        final String configurationRoot = System.getProperty(configProp, ctx.getInitParameter(configProp));
        final ConfigurationService configurationService = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext()).configurationService();


        handlerFactory = new TranslationHandlerFactory(configurationService, new XmlFilterChainBuilder((SAXTransformerFactory) TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null)), configurationRoot, config);
        configurationManager = ServletContextHelper.getInstance().getPowerApiContext(servletContext).configurationService();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/translation-configuration.xsd");
        configurationManager.subscribeTo(config,xsdURL, handlerFactory, TranslationConfig.class);
    }
}
