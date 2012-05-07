package com.rackspace.papi.jmx.mbeans;

import com.rackspace.papi.model.SystemModel;

import java.util.List;

/**
 * This interface is required for JMX instrumentation and must be postfixed with "MBean".
 *
 * @author fran
 */
public interface SystemConfigurationMBean {

   List<String> getLoadedFilters();

   SystemModel updatePowerProxy(SystemModel powerProxy);
}
