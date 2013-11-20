package com.rackspace.papi.commons.util.servlet.context;

import javax.servlet.ServletContext;

//TODO:WHAT?  is this named correctly?  or should it be ServletContextAdapter?
public interface ApplicationContextAdapter {

   void usingServletContext(ServletContext context);

   <T> T fromContext(Class<T> classToCastTo);

   <T> T fromContext(String refName, Class<T> classToCastTo);
}
