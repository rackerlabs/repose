package org.openrepose.core.services.context;

import javax.servlet.ServletContextEvent;

public interface ServletContextAware {
    void contextInitialized(ServletContextEvent sce);
    void contextDestroyed(ServletContextEvent sce);
}
