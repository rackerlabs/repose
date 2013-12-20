package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.rms.ResponseMessageService;
import com.rackspace.papi.service.rms.config.ResponseMessagingConfiguration;
import java.io.IOException;
import java.net.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component("responseMessageServiceContext")
public class ResponseMessageServiceContext implements ServiceContext<ResponseMessageService> {

    public static final String SERVICE_NAME = "powerapi:/services/rms";
     private static final Logger LOG = LoggerFactory.getLogger(ResponseMessageServiceContext.class);
    private final ResponseMessageService messageService;
    private final UpdateListener<ResponseMessagingConfiguration> configListener = new ResponseMessagingServiceListener();
    private final ServiceRegistry registry;
    private final ConfigurationService configurationService;

    @Autowired
    public ResponseMessageServiceContext(
            @Qualifier("responseMessagingService") ResponseMessageService messageService,
            @Qualifier("serviceRegistry") ServiceRegistry registry,
            @Qualifier("configurationManager") ConfigurationService configurationService) {
        this.messageService = messageService;
        this.registry = registry;
        this.configurationService = configurationService;
    }

    public void register() {
        if (registry != null) {
            registry.addService(this);
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
     try{
        
        URL xsdURL = getClass().getResource("/META-INF/schema/response-messaging/response-messaging.xsd");
        configurationService.subscribeTo("response-messaging.cfg.xml", xsdURL, configListener, ResponseMessagingConfiguration.class);
        if(!configurationService.getResourceResolver().resolve("response-messaging.cfg.xml").exists()){
          messageService.setInitialized();
        }
    register();
     } catch(IOException e){
        LOG.debug("Response messaging configuration file does not exist", e); 
     }
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

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ResponseMessagingConfiguration configurationObject) {
       
            messageService.setInitialized();
            messageService.updateConfiguration(configurationObject.getStatusCode());
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
