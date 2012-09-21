package com.rackspace.papi.components.translation;

import com.rackspace.papi.components.translation.config.TranslationConfig;
import com.rackspace.papi.components.translation.xslt.handlerchain.XsltHandlerChainBuilder;
import com.rackspace.papi.components.translation.xslt.xmlfilterchain.XsltFilterChainBuilder;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.servlet.InitParameter;
import java.io.IOException;
import javax.servlet.*;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import org.slf4j.Logger;
import org.xml.sax.XMLFilter;

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
        ServletContext servletContext = filterConfig.getServletContext();
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        final String configProp = InitParameter.POWER_API_CONFIG_DIR.getParameterName();
        final ServletContext ctx = filterConfig.getServletContext();
        final String configurationRoot = System.getProperty(configProp, ctx.getInitParameter(configProp));
        ConfigurationService manager = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext()).configurationService();
        //handlerFactory = new TranslationHandlerFactory<Templates>(manager, new XsltHandlerChainBuilder((SAXTransformerFactory) TransformerFactory.newInstance()), configurationRoot);
        handlerFactory = new TranslationHandlerFactory<XMLFilter>(manager, new XsltFilterChainBuilder((SAXTransformerFactory) TransformerFactory.newInstance()), configurationRoot);
        //handlerFactory = new TranslationHandlerFactory<Templates>(manager, new XsltFilterChainBuilder((SAXTransformerFactory) TransformerFactory.newInstance()), configurationRoot);
        configurationManager = ServletContextHelper.getInstance().getPowerApiContext(servletContext).configurationService();
        configurationManager.subscribeTo(config, handlerFactory, TranslationConfig.class);
    }
}
