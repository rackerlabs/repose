package org.openrepose.nodeservice.jmx;

import java.util.List;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;

public interface ConfigurationInformationMBean {
    String OBJECT_NAME = "org.openrepose.nodeservice.jmx:type=ConfigurationInformation";

    List<CompositeData> getFilterChain() throws OpenDataException;

}
