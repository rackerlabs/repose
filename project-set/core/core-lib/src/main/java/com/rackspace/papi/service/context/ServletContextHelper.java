package com.rackspace.papi.service.context;

import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.service.context.spring.SpringContextAdapter;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletContext;

public final class ServletContextHelper implements Serializable {

    public static final String SERVLET_CONTEXT_ATTRIBUTE_NAME = "PAPI_ServletContext";
    public static final String SERVLET_CONTEXT_HELPER = "PAPI_ServletContextHelper";
    public static final String SPRING_APPLICATION_CONTEXT_ATTRIBUTE_NAME = "PAPI_SpringApplicationContext";
    private static final Logger LOG = LoggerFactory.getLogger(ServletContextHelper.class);
    private final ApplicationContext context;

    public static ServletContextHelper configureInstance(ServletContext ctx, ApplicationContext applicationContext) {

        ServletContextHelper instance = new ServletContextHelper(applicationContext);
        instance.setPowerApiContext(ctx, applicationContext);
        ctx.setAttribute(SERVLET_CONTEXT_HELPER, instance);
        return instance;
    }

    public static ServletContextHelper getInstance(ServletContext ctx) {
        return (ServletContextHelper) ctx.getAttribute(SERVLET_CONTEXT_HELPER);
    }

    private ServletContextHelper(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return context;
    }

    public ContextAdapter getPowerApiContext() {
        return new SpringContextAdapter(context);
    }

    public void setPowerApiContext(ServletContext ctx, ApplicationContext applicationContext) {
        ctx.setAttribute(SPRING_APPLICATION_CONTEXT_ATTRIBUTE_NAME, applicationContext);
    }

    public ServicePorts getServerPorts() {
        return context.getBean("servicePorts", ServicePorts.class);
    }

    public ReposeInstanceInfo getReposeInstanceInfo() {
        return context.getBean("reposeInstanceInfo", ReposeInstanceInfo.class);

    }
}
