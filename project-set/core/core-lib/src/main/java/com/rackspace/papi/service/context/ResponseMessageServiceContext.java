package com.rackspace.papi.service.context;

import com.rackspace.papi.service.ServiceContext;
import com.rackspace.papi.service.rms.ResponseMessageService;
import com.rackspace.papi.service.rms.ResponseMessageServiceImpl;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

public class ResponseMessageServiceContext implements ServiceContext<ResponseMessageService> {

    public static final String SERVICE_NAME = "powerapi:/services/rms";
    
    private ResponseMessageServiceImpl messageService;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final ServletContext ctx = sce.getServletContext();

        messageService = new ResponseMessageServiceImpl(ServletContextHelper.getPowerApiContext(ctx).configurationService());
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
