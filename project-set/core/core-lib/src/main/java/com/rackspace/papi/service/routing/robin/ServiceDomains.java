package com.rackspace.papi.service.routing.robin;

import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.model.ServiceDomain;
import java.util.HashMap;
import java.util.Map;

public class ServiceDomains {
   private final Map<String, ServiceDomainWrapper> domains;
   
   public ServiceDomains(PowerProxy config) {
      domains = new HashMap<String, ServiceDomainWrapper>();
      
      for (ServiceDomain domain: config.getServiceDomain()) {
         domains.put(domain.getId(), new ServiceDomainWrapper(domain));
      }
   }
   
   public ServiceDomainWrapper getDomain(String id) {
      return domains.get(id);
   }
}
