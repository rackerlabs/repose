package com.rackspace.papi.service.context;

import javax.servlet.ServletContextEvent;

/**
 * Don't use this either, we should not be doing anything with this guy at all
 */
@Deprecated
public interface ServletContextAware {
    void contextInitialized(ServletContextEvent sce);
    void contextDestroyed(ServletContextEvent sce);
}
