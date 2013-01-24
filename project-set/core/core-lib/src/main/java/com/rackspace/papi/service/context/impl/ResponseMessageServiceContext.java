package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.rms.ResponseMessageService;
import com.rackspace.papi.service.rms.config.ResponseMessagingConfiguration;
import java.net.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;

@Component("responseMessageServiceContext")
public class ResponseMessageServiceContext implements ServiceContext<ResponseMessageService> {

    public static final String SERVICE_NAME = "powerapi:/services/rms";
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
        URL xsdURL = getClass().getResource("/META-INF/schema/response-messaging/response-messaging.xsd");
        configurationService.subscribeTo("response-messaging.cfg.xml", xsdURL, configListener, ResponseMessagingConfiguration.class);
        register();
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
            messageService.updateConfiguration(configurationObject.getStatusCode());
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
