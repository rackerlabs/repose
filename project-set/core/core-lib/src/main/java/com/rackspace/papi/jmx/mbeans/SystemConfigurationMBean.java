package com.rackspace.papi.jmx.mbeans;

import java.util.List;

/**
 * This interface is required for JMX instrumentation and must be postfixed with "MBean".
 *  
 * @author fran
 */
public interface SystemConfigurationMBean {

   public List<String> getLoadedFilters();
}
