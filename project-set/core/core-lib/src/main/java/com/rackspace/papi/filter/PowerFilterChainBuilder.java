/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.filter;

import com.rackspace.papi.filter.resource.ResourceConsumerMonitor;
import com.rackspace.papi.commons.util.Destroyable;
import java.util.List;
import javax.servlet.*;

/**
 *
 * @author zinic
 */
public class PowerFilterChainBuilder implements Destroyable {

   private final ResourceConsumerMonitor resourceConsumerMonitor;
   private final List<FilterContext> currentFilterChain;

   public PowerFilterChainBuilder(List<FilterContext> currentFilterChain) {
      this.currentFilterChain = currentFilterChain;
      resourceConsumerMonitor = new ResourceConsumerMonitor();
   }

   public ResourceConsumerMonitor getResourceConsumerMonitor() {
      return resourceConsumerMonitor;
   }
   
   public PowerFilterChain newPowerFilterChain(FilterChain containerFilterChain, ServletContext servletContext) {
      return new PowerFilterChain(currentFilterChain, containerFilterChain, servletContext, resourceConsumerMonitor);
   }

   @Override
   public void destroy() {
      for (FilterContext context : currentFilterChain) {
         context.destroy();
      }
   }
}
