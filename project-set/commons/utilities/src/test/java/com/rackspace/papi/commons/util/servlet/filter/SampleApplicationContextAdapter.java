package com.rackspace.papi.commons.util.servlet.filter;

import com.rackspace.papi.commons.util.servlet.context.ApplicationContextAdapter;

import javax.servlet.ServletContext;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: May 20, 2011
 * Time: 4:08:33 PM
 */
public class SampleApplicationContextAdapter implements ApplicationContextAdapter {

    @Override
    public void usingServletContext(ServletContext context) {
        //no-op
    }

    @Override
    public <T> T fromContext(Class<T> classToCastTo) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T fromContext(String refName, Class<T> classToCastTo) {
        throw new UnsupportedOperationException();
    }
}
