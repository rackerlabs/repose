package org.openrepose.core.services.jmx;

import java.util.List;
import java.util.Map;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;

public interface ConfigurationInformationMBean {
    String OBJECT_NAME = "org.openrepose.core.services.jmx:type=ConfigurationInformation";

    //TODO: I don't think this is useful any more...
    Map<String, List<CompositeData>> getPerNodeFilterInformation() throws OpenDataException;

    boolean isNodeReady(String clusterId, String nodeId);

}
