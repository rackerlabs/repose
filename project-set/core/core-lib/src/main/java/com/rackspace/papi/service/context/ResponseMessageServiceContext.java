package com.rackspace.papi.service.context;

import com.rackspace.papi.service.ServiceContext;
import com.rackspace.papi.service.rms.ResponseMessageService;
import com.rackspace.papi.service.rms.StatusCodeResponseMessageService;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.service.ServiceUnavailableException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseMessageServiceContext implements ServiceContext<ResponseMessageService> {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseMessageServiceContext.class);
    public static final String SERVICE_NAME = "powerapi:/services/rms";
    
    private StatusCodeResponseMessageService messageService;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final ServletContext ctx = sce.getServletContext();

        messageService = new StatusCodeResponseMessageService();

        try {
            messageService.configure(ServletContextHelper.getPowerApiContext(ctx).configurationService());
        } catch (ServiceUnavailableException sue) {
            LOG.error(sue.getMessage(), sue);
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
}
