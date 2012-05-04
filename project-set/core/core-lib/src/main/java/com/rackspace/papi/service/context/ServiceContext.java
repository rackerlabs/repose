package com.rackspace.papi.service.context;

import javax.servlet.ServletContextListener;

public interface ServiceContext<T> extends ServletContextListener {

    /**
     * Retrieves the canonical named path for this service. This name must match
     * the service's bound name in the Power API JNDI context.
     * 
     * @return 
     */
    String getServiceName();

    T getService();
}
