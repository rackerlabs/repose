package org.openrepose.core.service.reporting.jmx;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import java.util.Date;
import java.util.List;

public interface ReposeReportMBean {
    String OBJECT_NAME = "org.openrepose.core.service.reporting:type=ReposeReport";

    Date getLastReset();

    String getTotal400sReposeToClient();
    String getTotal500sReposeToClient();

    List<CompositeData> getDestinationInfo() throws OpenDataException;        
}
