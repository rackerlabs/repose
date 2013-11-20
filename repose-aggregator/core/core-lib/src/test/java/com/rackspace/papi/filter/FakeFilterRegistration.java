package com.rackspace.papi.filter;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * @author fran
 */
public class FakeFilterRegistration implements FilterRegistration.Dynamic {
    public FakeFilterRegistration() {
    }
    
    @Override
    public void setAsyncSupported(boolean isAsyncSupported) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... servletNames) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<String> getServletNameMappings() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... urlPatterns) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<String> getUrlPatternMappings() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getClassName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getInitParameter(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<String> setInitParameters(Map<String, String> initParameters) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, String> getInitParameters() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
