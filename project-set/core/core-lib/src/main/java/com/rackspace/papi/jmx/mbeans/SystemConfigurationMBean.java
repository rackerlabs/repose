package com.rackspace.papi.jmx.mbeans;

import com.rackspace.papi.model.Host;
import com.rackspace.papi.model.PowerProxy;

import java.util.List;

/**
 * This interface is required for JMX instrumentation and must be postfixed with "MBean".
 *  
 * @author fran
 */
public interface SystemConfigurationMBean {

   public List<String> getLoadedFilters();

   public PowerProxy updatePowerProxy(PowerProxy powerProxy);
}
