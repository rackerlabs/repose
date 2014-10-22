package org.openrepose.core.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * Defining the common interface for something that provides a Spring Context
 */
public interface SpringProvider {

    /**
     * Just gets you the core application context
     *
     * @return the core application context
     */
    public ApplicationContext getCoreContext();

}
