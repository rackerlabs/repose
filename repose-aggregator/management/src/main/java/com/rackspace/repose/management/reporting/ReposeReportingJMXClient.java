package com.rackspace.repose.management.reporting;

import com.rackspace.papi.service.reporting.jmx.ReposeReportMBean;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: fran
 * Date: Oct 23, 2012
 * Time: 4:51:15 PM
 */
public class ReposeReportingJMXClient implements ReposeReportMBean {

    private final ReposeReportMBean reposeReportMBean;

    public ReposeReportingJMXClient() throws IOException, MalformedObjectNameException {

        final String port = getReposeJMXPort();
        final String jmxRmiUrl = "service:jmx:rmi:///jndi/rmi://:" + port + "/jmxrmi";
        final JMXServiceURL url = new JMXServiceURL(jmxRmiUrl);
        final JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        final MBeanServerConnection reposeConnection = jmxc.getMBeanServerConnection();

        reposeReportMBean = JMX.newMBeanProxy(reposeConnection,
                new ObjectName(ReposeReportMBean.OBJECT_NAME),
                ReposeReportMBean.class,
                true);
    }

    private String getReposeJMXPort() {
        return System.getProperty("com.sun.management.jmxremote.port", "");
    }

    @Override
    public Date getLastReset() {
        return reposeReportMBean.getLastReset();
    }

    @Override
    public String getTotal400sReposeToClient() {
        return reposeReportMBean.getTotal400sReposeToClient();
    }

    @Override
    public String getTotal500sReposeToClient() {
        return reposeReportMBean.getTotal500sReposeToClient();
    }

    @Override
    public List<CompositeData> getDestinationInfo() throws OpenDataException {
        return reposeReportMBean.getDestinationInfo();
    }
}
