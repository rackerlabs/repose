package com.rackspace.papi.service.context;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.service.ServiceContext;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.rms.ResponseMessageService;
import com.rackspace.papi.service.rms.ResponseMessageServiceImpl;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.service.rms.config.ResponseMessagingConfiguration;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

public class ResponseMessageServiceContext implements ServiceContext<ResponseMessageService> {

   public static final String SERVICE_NAME = "powerapi:/services/rms";

   private ResponseMessageServiceImpl messageService;

   private final UpdateListener<ResponseMessagingConfiguration> configListener = new ResponseMessagingServiceListener();

   @Override
   public void contextInitialized(ServletContextEvent sce) {
      final ServletContext ctx = sce.getServletContext();
      final ConfigurationService configurationService = ServletContextHelper.getPowerApiContext(ctx).configurationService();

      messageService = new ResponseMessageServiceImpl();

      configurationService.subscribeTo("response-messaging.cfg.xml", configListener, ResponseMessagingConfiguration.class);
   }

   @Override
   public String getServiceName() {
      return SERVICE_NAME;
   }

   @Override
   public ResponseMessageService getService() {
      return messageService;
   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
      messageService.destroy();
   }

   private class ResponseMessagingServiceListener implements UpdateListener<ResponseMessagingConfiguration> {

      @Override
      public void configurationUpdated(ResponseMessagingConfiguration configurationObject) {
         messageService.updateConfiguration(configurationObject.getStatusCode());
      }
   }
}
