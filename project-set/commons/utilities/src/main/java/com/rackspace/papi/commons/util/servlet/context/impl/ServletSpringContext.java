/*
 *  Copyright 2010 Rackspace.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package com.rackspace.papi.commons.util.servlet.context.impl;

import com.rackspace.papi.commons.util.servlet.context.ApplicationContextAdapter;
import javax.servlet.ServletContext;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 *

 */
public class ServletSpringContext implements ApplicationContextAware, ApplicationContextAdapter {

    private ApplicationContext applicationContext;

    @Override
    public synchronized void setApplicationContext(ApplicationContext ac) {
        if (applicationContext == null) {
            applicationContext = ac;
        }
    }

    @Override
    public synchronized void usingServletContext(ServletContext context) {
        if (applicationContext == null) {
            applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(context);
        }
    }

    @Override
    public <T> T fromContext(Class<T> classToCastTo) {
        return applicationContext.getBean(classToCastTo);
    }

    @Override
    public <T> T fromContext(String refName, Class<T> classToCastTo) {
        return applicationContext.getBean(refName, classToCastTo);
    }
}
