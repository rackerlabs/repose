package com.rackspace.papi.service.datastore.distributed.impl.distributed.servlet;

import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.datastore.DatastoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/*
 * Builds servlet context for the distributed datastore servlet
 */
@Component("distributedDatastoreServletContextManager")
public class DistributedDatastoreServletContextManager implements ServletContextListener, ApplicationContextAware {
    private static final Logger LOG = LoggerFactory.getLogger(DistributedDatastoreServletContextManager.class);

   private DatastoreService datastoreService;
   private ApplicationContext applicationContext;
   private ReposeInstanceInfo instanceInfo;

   @Override
   public void contextInitialized(ServletContextEvent sce) {

      final ServletContext servletContext = sce.getServletContext();
      ServletContextHelper.configureInstance(
              servletContext,
              applicationContext);
      servletContext.setAttribute("datastoreService", datastoreService);
      configureReposeInfo(applicationContext);
   }
   
   public void configureReposeInfo(ApplicationContext context) {
       //TODO: dont' configure instance info here, get it from the parent spring context
      if (context == null) {
         return;
      }

      //JIMMY NOOOOOOOOOOOo
      ReposeInstanceInfo reposeInstanceInfo = context.getBean("reposeInstanceInfo", ReposeInstanceInfo.class);
      reposeInstanceInfo.setClusterId(instanceInfo.getClusterId());
      reposeInstanceInfo.setNodeId(instanceInfo.getNodeId());
   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
   }

   public void setDatastoreSystemProperties(DatastoreService datastore, ReposeInstanceInfo instanceInfo) {
      this.datastoreService = datastore;
      this.instanceInfo = instanceInfo;
   }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
