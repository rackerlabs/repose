package org.openrepose.cli.command.datastore.local;

import com.rackspace.papi.service.datastore.impl.ehcache.ReposeLocalCacheMBean;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;

public class ReposeJMXClient implements ReposeLocalCacheMBean {

    private final ReposeLocalCacheMBean reposeLocalCacheMBeanProxy;

    public ReposeJMXClient(String port) throws IOException, MalformedObjectNameException {

        final String jmxRmiUrl = "service:jmx:rmi:///jndi/rmi://:" + port + "/jmxrmi";
        final JMXServiceURL url = new JMXServiceURL(jmxRmiUrl);
        final JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        final MBeanServerConnection reposeConnection = jmxc.getMBeanServerConnection();

        reposeLocalCacheMBeanProxy = JMX.newMBeanProxy(reposeConnection,
                                                       new ObjectName(ReposeLocalCacheMBean.OBJECT_NAME),
                                                       ReposeLocalCacheMBean.class,
                                                       true);
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
    public boolean removeLimits(String encodedUserId) {
        return reposeLocalCacheMBeanProxy.removeLimits(encodedUserId);
    }

    @Override
    public void removeAllCacheData() {
        reposeLocalCacheMBeanProxy.removeAllCacheData();
    }
}
