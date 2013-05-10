package com.rackspace.repose.management.cache;

import com.rackspace.papi.service.datastore.impl.ehcache.ReposeLocalCacheMBean;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: fran
 * Date: Oct 23, 2012
 * Time: 4:34:18 PM
 */
public class ReposeCacheJMXClient implements ReposeLocalCacheMBean {

    private final ReposeLocalCacheMBean reposeLocalCacheMBeanProxy;

    public ReposeCacheJMXClient() throws IOException, MalformedObjectNameException {

        final String port = getReposeJMXPort();
        final String jmxRmiUrl = "service:jmx:rmi:///jndi/rmi://:" + port + "/jmxrmi";
        final JMXServiceURL url = new JMXServiceURL(jmxRmiUrl);
        final JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        final MBeanServerConnection reposeConnection = jmxc.getMBeanServerConnection();

        reposeLocalCacheMBeanProxy = JMX.newMBeanProxy(reposeConnection,
                                                       new ObjectName(ReposeLocalCacheMBean.OBJECT_NAME),
                                                       ReposeLocalCacheMBean.class,
                                                       true);
    }

    private String getReposeJMXPort() {
        return System.getProperty("com.sun.management.jmxremote.port", "");
    }

    @Override
    public boolean removeTokenAndRoles(String tenantId, String token) {
        return reposeLocalCacheMBeanProxy.removeTokenAndRoles(tenantId, token);
    }

    @Override
    public boolean removeGroups(String tenantId, String token) {
        return reposeLocalCacheMBeanProxy.removeGroups(tenantId, token);
    }

    @Override
    public boolean removeLimits(String userId) {
        return reposeLocalCacheMBeanProxy.removeLimits(userId);
    }

    @Override
    public void removeAllCacheData() {
        reposeLocalCacheMBeanProxy.removeAllCacheData();
    }
}
