package com.rackspace.config.manip.jmx;

import com.rackspace.papi.jmx.mbeans.SystemConfigurationMBean;
import com.rackspace.papi.model.PowerProxy;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Client {
   private static final String OBJECT_NAME = "com.rackspace.papi.jmx.mbeans:type=SystemConfiguration";
   private static final String JMX_RMI_URL = "service:jmx:rmi:///jndi/rmi://:9999/jmxrmi";

   private final JMXServiceURL url;
   private final JMXConnector jmxc;

   private final MBeanServerConnection mbsc;
   private final ClientListener listener;
   private final SystemConfigurationMBean systemConfigurationMbeanProxy;
   private final ObjectName mbeanName;

   public Client() {
      System.out.println("\nCreate an RMI connector client and connect it to the RMI connector server");

      try {
         this.url = new JMXServiceURL(JMX_RMI_URL);
      } catch (MalformedURLException e) {
         throw new ConnectionException("Problem creating JMXServiceURL", e);
      }

      try {
         this.jmxc = JMXConnectorFactory.connect(url, null);
      } catch (IOException e) {
         throw new ConnectionException("Problem creating JMXConnector", e);
      }

      this.listener = new ClientListener();
      try {
         this.mbeanName = new ObjectName(OBJECT_NAME);
      } catch (MalformedObjectNameException e) {
         throw new ConnectionException("Problem creating ObjectName", e);
      }

      try {
         this.mbsc = jmxc.getMBeanServerConnection();
      } catch (IOException e) {
         throw new ConnectionException("Problem getting MBeanServerConnection", e);
      }

      systemConfigurationMbeanProxy = JMX.newMBeanProxy(mbsc, mbeanName, SystemConfigurationMBean.class, true);
            
      printConnectInfo();
   }
     
   public PowerProxy updateReposeSystemConfiguration(PowerProxy powerProxy) {

      List<String> filters = systemConfigurationMbeanProxy.getLoadedFilters();

      PowerProxy returnedPowerProxy = systemConfigurationMbeanProxy.updatePowerProxy(powerProxy);

      System.out.println("returned: " + powerProxy.getHost().get(0).getHostname());

      return returnedPowerProxy;
   }

   private void printConnectInfo() {

      try {
         System.out.println("\nDomains:");
         String domains[] = mbsc.getDomains();
         Arrays.sort(domains);
         for (String domain : domains) {
            System.out.println("\tDomain = " + domain);
         }
         System.out.println("\nMBeanServer default domain = " + mbsc.getDefaultDomain());

         System.out.println("\nMBean count = " + mbsc.getMBeanCount());
         System.out.println("\nQuery MBeanServer MBeans:");
         Set<ObjectName> names = new TreeSet<ObjectName>(mbsc.queryNames(null, null));
         for (ObjectName name : names) {
            System.out.println("\tObjectName = " + name);
         }
      } catch (IOException e) {
         throw new ConnectionException("Problem printing connect info", e);
      }
   }
}