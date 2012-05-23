package com.rackspace.papi.commons.util.servlet.filter;

import com.rackspace.papi.commons.util.servlet.InitParameter;
import com.rackspace.papi.commons.util.servlet.context.ApplicationContextAdapter;
import com.rackspace.papi.commons.util.servlet.context.exceptions.ContextAdapterResolutionException;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

/**
 * This class serves as an abstraction parent to any number of IOC Containers herefore referenced as the application
 * context. This is not a naming context in the JNDI sense nor is it the Servlet context.
 *
 */
public abstract class ApplicationContextAwareFilter implements Filter {

   private ApplicationContextAdapter applicationContextAdapter;

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {

      final String adapterClassName = filterConfig.getInitParameter(InitParameter.APP_CONTEXT_ADAPTER_CLASS.getParameterName());

      if (adapterClassName != null && !"".equals(adapterClassName)) {
         try {
            final Object freshAdapter = Class.forName(adapterClassName).newInstance();

            if (freshAdapter instanceof ApplicationContextAdapter) {
               applicationContextAdapter = (ApplicationContextAdapter) freshAdapter;
               applicationContextAdapter.usingServletContext(filterConfig.getServletContext());
            } else {
               throw new ContextAdapterResolutionException("Unknown application context adapter class: " + adapterClassName);
            }
         } catch (Exception ex) {
            throw new ContextAdapterResolutionException("Failure on ApplicationContextAwareFilter.init(...). Reason: " + ex.getMessage(), ex);
         }
      }
   }

   public ApplicationContextAdapter getAppContext() {
      return applicationContextAdapter;
   }
}
