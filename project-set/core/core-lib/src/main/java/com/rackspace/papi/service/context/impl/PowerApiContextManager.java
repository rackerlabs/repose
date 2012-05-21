package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.context.banner.PapiBanner;
import com.rackspace.papi.service.context.spring.SpringContextAdapterProvider;
import com.rackspace.papi.service.deploy.ArtifactManagerServiceContext;
import com.rackspace.papi.service.threading.impl.ThreadingServiceContext;
import com.rackspace.papi.spring.SpringConfiguration;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class PowerApiContextManager implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(PowerApiContextManager.class);
    private ApplicationContext applicationContext;

    public PowerApiContextManager() {
        //applicationContext = new ClassPathXmlApplicationContext(APPLICATION_CONTEXT_CONFIG);
        applicationContext = new AnnotationConfigApplicationContext(SpringConfiguration.class);
    }

    public PowerApiContextManager setPorts(ServicePorts ports) {
        ServicePorts servicePorts = applicationContext.getBean("servicePorts", ServicePorts.class);
        servicePorts.clear();
        servicePorts.addAll(ports);
        return this;
    }

    private void intializeServices(ServletContextEvent sce) {
        ServletContextHelper helper = ServletContextHelper.getInstance();
        ContextAdapter ca = helper.getPowerApiContext(sce.getServletContext());

        ca.getContext(ThreadingServiceContext.class).contextInitialized(sce);
        ca.getContext(EventManagerServiceContext.class).contextInitialized(sce);
        ca.getContext(ConfigurationServiceContext.class).contextInitialized(sce);
        ca.getContext(ContainerServiceContext.class).contextInitialized(sce);
        ca.getContext(RoutingServiceContext.class).contextInitialized(sce);
        ca.getContext(LoggingServiceContext.class).contextInitialized(sce);
        PapiBanner.print(LOG);
        ca.getContext(ResponseMessageServiceContext.class).contextInitialized(sce);
        // TODO:Refactor - This service should be bound to a fitler-chain specific JNDI context
        ca.getContext(DatastoreServiceContext.class).contextInitialized(sce);
        ca.getContext(ClassLoaderServiceContext.class).contextInitialized(sce);
        ca.getContext(ArtifactManagerServiceContext.class).contextInitialized(sce);
        ca.getContext(FilterChainGCServiceContext.class).contextInitialized(sce);

    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final ServletContext servletContext = sce.getServletContext();

        // Most bootstrap steps require or will try to load some kind of
        // configuration so we need to set our naming context in the servlet context
        // first before anything else
        ServletContextHelper.configureInstance(
                new SpringContextAdapterProvider(applicationContext),
                servletContext,
                applicationContext);

        intializeServices(sce);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServiceRegistry registry = applicationContext.getBean("serviceRegistry", ServiceRegistry.class);
        for (ServiceContext ctx : registry.getServices()) {
            ctx.contextDestroyed(sce);
        }
    }
}
