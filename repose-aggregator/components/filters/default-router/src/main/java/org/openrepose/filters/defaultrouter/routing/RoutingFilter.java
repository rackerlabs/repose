package org.openrepose.filters.defaultrouter.routing;

import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.systemmodel.SystemModel;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;

@Deprecated
@Named
public class RoutingFilter implements Filter {

    private final ConfigurationService configurationService;
    private final RoutingHandlerFactory handlerFactory;
    private ApplicationContext applicationContext;

    @Inject
    public RoutingFilter(ConfigurationService configurationService,
                         RoutingHandlerFactory handlerFactory) {
        this.configurationService = configurationService;
        this.handlerFactory = handlerFactory;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        configurationService.subscribeTo(filterConfig.getFilterName(),"system-model.cfg.xml", handlerFactory, SystemModel.class);
    }
}
