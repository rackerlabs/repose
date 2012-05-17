package com.rackspace.papi.filter;

import com.rackspace.papi.filter.resource.ResourceConsumerCounter;
import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import java.util.List;
import javax.servlet.*;

/**
 *
 * @author zinic
 */
public class PowerFilterChainBuilder implements Destroyable {

   private final ResourceConsumerCounter resourceConsumerMonitor;
   private final List<FilterContext> currentFilterChain;
   private final ReposeCluster domain;
   private final Node localhost;
   

   public PowerFilterChainBuilder(ReposeCluster domain, Node localhost, List<FilterContext> currentFilterChain) {
      this.currentFilterChain = currentFilterChain;
      resourceConsumerMonitor = new ResourceConsumerCounter();
      this.domain = domain;
      this.localhost = localhost;
   }

   public ResourceConsumerCounter getResourceConsumerMonitor() {
      return resourceConsumerMonitor;
   }
   
   public PowerFilterChain newPowerFilterChain(FilterChain containerFilterChain, ServletContext servletContext) {
      return new PowerFilterChain(domain, localhost, currentFilterChain, containerFilterChain, servletContext, resourceConsumerMonitor);
   }
   
   public ReposeCluster getReposeCluster() {
      return domain;
   }
   
   public Node getLocalhost() {
      return localhost;
   }

   @Override
   public void destroy() {
      for (FilterContext context : currentFilterChain) {
         context.destroy();
      }
   }
}
