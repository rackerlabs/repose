package com.rackspace.papi.service.context;

import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.domain.ServicePorts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletContext;

public final class ServletContextHelper {

    public static final String SERVLET_CONTEXT_ATTRIBUTE_NAME = "PAPI_ServletContext";
    public static final String SPRING_APPLICATION_CONTEXT_ATTRIBUTE_NAME = "PAPI_SpringApplicationContext";
    private static final Logger LOG = LoggerFactory.getLogger(ServletContextHelper.class);
    private static final Object LOCK = new Object();
    private static ServletContextHelper instance = null;
    private final ContextAdapterProvider adapterProvider;

    public static ServletContextHelper configureInstance(ContextAdapterProvider adapterProvider, ServletContext ctx, ApplicationContext applicationContext) {
        synchronized (LOCK) {
            if (adapterProvider != null) {
                LOG.debug("Configuring ContextAdapterProvider: " + adapterProvider.getClass().getName());
                instance = new ServletContextHelper(adapterProvider);
                instance.setPowerApiContext(ctx, applicationContext);
            }
            return instance;
        }
    }

    public static ServletContextHelper getInstance() {
        synchronized (LOCK) {
            return instance;
        }
    }

    private ServletContextHelper() {
        this.adapterProvider = null;
    }

    private ServletContextHelper(ContextAdapterProvider adapterProvider) {
        this.adapterProvider = adapterProvider;
    }

    public ApplicationContext getApplicationContext(ServletContext ctx) {
        return (ApplicationContext) ctx.getAttribute(SPRING_APPLICATION_CONTEXT_ATTRIBUTE_NAME);
    }

    public ContextAdapter getPowerApiContext(ServletContext ctx) {
        return adapterProvider.newInstance(null);
    }

    public ContextAdapter getPowerApiContext() {
        return adapterProvider.newInstance(null);
    }

    public void setPowerApiContext(ServletContext ctx, ApplicationContext applicationContext) {
        ctx.setAttribute(SPRING_APPLICATION_CONTEXT_ATTRIBUTE_NAME, applicationContext);
    }

    public ServicePorts getServerPorts(ServletContext ctx) {
        return getApplicationContext(ctx).getBean("servicePorts", ServicePorts.class);
    }

    public ReposeInstanceInfo getReposeInstanceInfo(ServletContext ctx) {
        return getApplicationContext(ctx).getBean("reposeInstanceInfo", ReposeInstanceInfo.class);

    }
}
