package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.rms.ResponseMessageService;
import com.rackspace.papi.service.rms.ResponseMessageServiceImpl;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.rms.config.ResponseMessagingConfiguration;
import javax.annotation.Resource;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Component;

@Component("responseMessageServiceContext")
public class ResponseMessageServiceContext implements ServiceContext<ResponseMessageService> {

   public static final String SERVICE_NAME = "powerapi:/services/rms";
   private final ResponseMessageService messageService;
   private final UpdateListener<ResponseMessagingConfiguration> configListener = new ResponseMessagingServiceListener();
   
   @Autowired
   public ResponseMessageServiceContext(@Qualifier("responseMessagingService") ResponseMessageService messageService) {
      this.messageService = messageService;
   }
   
   @Override
   public void contextInitialized(ServletContextEvent sce) {
      final ServletContext ctx = sce.getServletContext();
      final ConfigurationService configurationService = ServletContextHelper.getInstance().getPowerApiContext(ctx).configurationService();

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
