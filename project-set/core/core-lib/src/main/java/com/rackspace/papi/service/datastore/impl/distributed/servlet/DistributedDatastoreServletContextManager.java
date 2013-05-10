/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.service.datastore.impl.distributed.servlet;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.ServletContextAware;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.context.impl.ConfigurationServiceContext;
import com.rackspace.papi.service.context.impl.EventManagerServiceContext;
import com.rackspace.papi.service.context.impl.LoggingServiceContext;
import com.rackspace.papi.service.context.impl.ReportingServiceContext;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.impl.distributed.cluster.DistributedDatastoreServiceClusterContext;
import com.rackspace.papi.service.threading.impl.ThreadingServiceContext;
import com.rackspace.papi.servlet.InitParameter;
import com.rackspace.papi.spring.SpringConfiguration;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/*
 * Builds servlet context for the distributed datastore servlet
 */
public class DistributedDatastoreServletContextManager implements ServletContextListener {

   private DatastoreService datastoreService;
   private AnnotationConfigApplicationContext applicationContext;
   private static final String DEFAULT_CONNECTION_FRAMEWORK = "jerseyRequestProxyService";
   private ReposeInstanceInfo instanceInfo;

   public DistributedDatastoreServletContextManager() {
   }

   @Override
   public void contextInitialized(ServletContextEvent sce) {

      final ServletContext servletContext = sce.getServletContext();

      applicationContext = new AnnotationConfigApplicationContext(SpringConfiguration.class);

      String connectionFrameworkProp = "CONNECTION_FRAMEWORK";
      final String connectionFramework = System.getProperty(connectionFrameworkProp, servletContext.getInitParameter(connectionFrameworkProp));
      final String beanName = StringUtilities.isNotBlank(connectionFramework) ? connectionFramework + "RequestProxyService" : null;
      if (StringUtilities.isNotBlank(beanName) && applicationContext.containsBean(beanName)) {
         //LOG.info("Using connection framework: " + beanName);
         applicationContext.registerAlias(beanName, "requestProxyService");
      } else {
         //LOG.info("Using default connection framework: " + DEFAULT_CONNECTION_FRAMEWORK);
         applicationContext.registerAlias(DEFAULT_CONNECTION_FRAMEWORK, "requestProxyService");
      }

      ServletContextHelper.configureInstance(
              servletContext,
              applicationContext);
      servletContext.setAttribute("datastoreService", datastoreService);
      configureReposeInfo(applicationContext);
      initializeService(sce);
   }
   
   public void configureReposeInfo(ApplicationContext context) {
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

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
      //contextInitialized = false;

//      Map<String, ServletContextAware> contextAwareBeans = applicationContext.getBeansOfType(ServletContextAware.class);
//
//      for (ServletContextAware bean : contextAwareBeans.values()) {
//         bean.contextDestroyed(sce);
//      }

      ServiceRegistry registry = applicationContext.getBean("serviceRegistry", ServiceRegistry.class);
      for (ServiceContext ctx : registry.getServices()) {
         ctx.contextDestroyed(sce);
      }

      //LOG.info("Shutting down Spring application context");
      applicationContext.close();
   }

   public void setDatastoreSystemProperties(DatastoreService datastore, ReposeInstanceInfo instanceInfo) {
      this.datastoreService = datastore;
      this.instanceInfo = instanceInfo;
   }

   private void initializeService(ServletContextEvent sce) {

      ServletContextHelper helper = ServletContextHelper.getInstance(sce.getServletContext());
      ContextAdapter ca = helper.getPowerApiContext();

      ca.getContext(ThreadingServiceContext.class).contextInitialized(sce);
      ca.getContext(EventManagerServiceContext.class).contextInitialized(sce);
      ca.getContext(ConfigurationServiceContext.class).contextInitialized(sce);
      ca.getContext(LoggingServiceContext.class).contextInitialized(sce);
      ca.getContext(ReportingServiceContext.class).contextInitialized(sce);
      ca.getContext(DistributedDatastoreServiceClusterContext.class).contextInitialized(sce);
   }

   private AnnotationConfigApplicationContext initApplicationContext(ServletContext servletContext) {
      final String connectionFrameworkProp = InitParameter.CONNECTION_FRAMEWORK.getParameterName();
      final String connectionFramework = System.getProperty(connectionFrameworkProp, servletContext.getInitParameter(connectionFrameworkProp));
      final String beanName = StringUtilities.isNotBlank(connectionFramework) ? connectionFramework + "RequestProxyService" : null;

      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SpringConfiguration.class);
      if (StringUtilities.isNotBlank(beanName) && context.containsBean(beanName)) {
         //LOG.info("Using connection framework: " + beanName);
         context.registerAlias(beanName, "requestProxyService");
      } else {
         //LOG.info("Using default connection framework: " + DEFAULT_CONNECTION_FRAMEWORK);
         context.registerAlias(DEFAULT_CONNECTION_FRAMEWORK, "requestProxyService");
      }


      Thread.currentThread().setName("Dist-Datastore");
      context.getBean("exporter");

      return context;
   }
}
