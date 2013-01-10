package com.rackspace.papi.jmx;

import java.util.List;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;

public interface ConfigurationInformationMBean {
    String OBJECT_NAME = "com.rackspace.papi.jmx:type=ConfigurationInformation";

    List<CompositeData> getFilterChain() throws OpenDataException;
}
