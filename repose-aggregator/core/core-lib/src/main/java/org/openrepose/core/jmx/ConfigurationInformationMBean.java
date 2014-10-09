package org.openrepose.core.jmx;

import java.util.List;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;

public interface ConfigurationInformationMBean {
    String OBJECT_NAME = "org.openrepose.core.jmx:type=ConfigurationInformation";

    List<CompositeData> getFilterChain() throws OpenDataException;

}
