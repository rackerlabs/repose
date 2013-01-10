package com.rackspace.papi.service.context;

import javax.servlet.ServletContextEvent;

public interface ServletContextAware {
    public void contextInitialized(ServletContextEvent sce);

    public void contextDestroyed(ServletContextEvent sce);
    
}
