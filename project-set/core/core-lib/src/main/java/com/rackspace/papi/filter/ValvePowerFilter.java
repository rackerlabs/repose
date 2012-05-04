package com.rackspace.papi.filter;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

/**
 *
 * @author zinic
 */
public class ValvePowerFilter extends PowerFilter {

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {
      super.init(new FilterConfigWrapper(filterConfig));
   }
}