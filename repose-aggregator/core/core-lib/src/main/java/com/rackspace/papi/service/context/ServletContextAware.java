package com.rackspace.papi.service.context;

import javax.servlet.ServletContextEvent;

public interface ServletContextAware {
    void contextInitialized(ServletContextEvent sce);
    void contextDestroyed(ServletContextEvent sce);
}
