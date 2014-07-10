package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.HttpsURLConnectionSslInitializer;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.ServletContextAware;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.context.banner.PapiBanner;
import com.rackspace.papi.servlet.InitParameter;
import com.rackspace.papi.spring.SpringWithServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Map;

public class PowerApiContextManager implements ServletContextListener {

   private static final Logger LOG = LoggerFactory.getLogger(PowerApiContextManager.class);
   private AbstractApplicationContext applicationContext;
   private ServicePorts ports;
   private boolean contextInitialized = false;
   private ReposeInstanceInfo instanceInfo;

   public PowerApiContextManager() {
   }

   public PowerApiContextManager setPorts(ServicePorts ports, ReposeInstanceInfo instanceInfo) {
      this.ports = ports;
      this.instanceInfo = instanceInfo;
      configurePorts(applicationContext);
      configureReposeInfo(applicationContext);
      return this;
   }

   private AbstractApplicationContext initApplicationContext(ServletContext servletContext) {
      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SpringWithServices.class);

      configurePorts(context);
      configureReposeInfo(context);
      Thread.currentThread().setName(instanceInfo.toString());
      context.getBean("exporter");

      return context;

   }

   private void configurePorts(ApplicationContext context) {
      if (ports == null || context == null) {
         return;
      }
      ServicePorts servicePorts = context.getBean("servicePorts", ServicePorts.class);
      servicePorts.clear();
      servicePorts.addAll(ports);
   }

   private void configureReposeInfo(ApplicationContext context) {
      if (instanceInfo == null) {

         String clusterId = System.getProperty("repose-cluster-id");
         String nodeId = System.getProperty("repose-node-id");
         instanceInfo = new ReposeInstanceInfo(clusterId, nodeId);
      }
      if (context == null) {
         return;
      }


      ReposeInstanceInfo reposeInstanceInfo = context.getBean("reposeInstanceInfo", ReposeInstanceInfo.class);
      reposeInstanceInfo.setClusterId(instanceInfo.getClusterId());
      reposeInstanceInfo.setNodeId(instanceInfo.getNodeId());
   }

   private void intializeServices(ServletContextEvent sce) {
      ServletContextHelper helper = ServletContextHelper.getInstance(sce.getServletContext());
      ContextAdapter ca = helper.getPowerApiContext();

      ca.getContext(ConfigurationServiceContext.class).contextInitialized(sce);
      ca.getContext(ContainerServiceContext.class).contextInitialized(sce);
      PapiBanner.print(LOG);
      // TODO:Refactor - This service should be bound to a fitler-chain specific JNDI context
      ca.getContext(RequestHeaderServiceContext.class).contextInitialized(sce);
      ca.getContext(ResponseHeaderServiceContext.class).contextInitialized(sce);
      ca.getContext(DistributedDatastoreServiceContext.class).contextInitialized(sce);
      ca.getContext(MetricsServiceContext.class).contextInitialized( sce );
      ca.getContext(HttpConnectionPoolServiceContext.class).contextInitialized(sce);
      ca.getContext(AkkaServiceClientContext.class).contextInitialized(sce);

      Map<String, ServletContextAware> contextAwareBeans = applicationContext.getBeansOfType(ServletContextAware.class);

      for (ServletContextAware bean : contextAwareBeans.values()) {
         bean.contextInitialized(sce);
      }

   }

   private boolean isManagementServerEnabled() {
      return System.getProperty(InitParameter.MANAGEMENT_PORT.getParameterName()) != null;
   }

   @Override
   public void contextInitialized(ServletContextEvent sce) {
      final ServletContext servletContext = sce.getServletContext();

      final String insecureProp = InitParameter.INSECURE.getParameterName();
      final String insecure = System.getProperty(insecureProp, servletContext.getInitParameter(insecureProp));

      if (StringUtilities.nullSafeEqualsIgnoreCase(insecure, "true")) {
         new HttpsURLConnectionSslInitializer().allowAllServerCerts();
      }

      applicationContext = initApplicationContext(servletContext);

      //Allows Repose to set any header to pass to the origin service. Namely the "Via" header
      System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

      // Most bootstrap steps require or will try to load some kind of
      // configuration so we need to set our naming context in the servlet context
      // first before anything else
      ServletContextHelper.configureInstance(
              servletContext,
              applicationContext);

      intializeServices(sce);
      servletContext.setAttribute("powerApiContextManager", this);
      contextInitialized = true;
   }

   public boolean isContextInitialized() {
      return contextInitialized;
   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
      contextInitialized = false;

      Map<String, ServletContextAware> contextAwareBeans = applicationContext.getBeansOfType(ServletContextAware.class);

      for (ServletContextAware bean : contextAwareBeans.values()) {
         bean.contextDestroyed(sce);
      }

      ServiceRegistry registry = applicationContext.getBean("serviceRegistry", ServiceRegistry.class);
      for (ServiceContext ctx : registry.getServices()) {
         ctx.contextDestroyed(sce);
      }

      LOG.info("Shutting down Spring application context");
      applicationContext.close();

   }
}
