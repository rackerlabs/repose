package com.rackspace.papi.jmx.agents;

import com.rackspace.papi.jmx.mbeans.SystemConfiguration;
import com.rackspace.papi.model.Host;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.lang.management.ManagementFactory;

/**
 * "A JMX agent is a standard management agent that directly controls resources and makes them available to
 *  remote management applications."
 *  source: http://docs.oracle.com/javase/6/docs/technotes/guides/jmx/overview/architecture.html#wp998959
 *
 *  For the time being, this JMX agent is very specific just to make use of the JMX technology in a very simple form.
 *  once we do further analysis to determine which parts of Repose should be instrumented in JMX this class will
 *  need to change.
 *  
 * @author fran
 */
public class SystemJmxAgent {

   private static final Logger LOG = LoggerFactory.getLogger(SystemJmxAgent.class);
   private static final String OBJECT_NAME = "com.rackspace.papi.jmx.mxbeans:type=SystemConfiguration";
   private final Host localHost;
   private ObjectName name;

   public SystemJmxAgent(Host localHost) {
      this.localHost = localHost;

      try {
         name = new ObjectName(OBJECT_NAME);
      } catch (MalformedObjectNameException e) {
         LOG.error("Error creating ObjectName " + OBJECT_NAME, e);
      }
   }

   public void registerMBean() {
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

      // Attempt to unregister first since we need to handle the case where the power proxy config is modified.
      // If the mbean is not registered yet an exception will be thrown, but we catch it and just log info about it
      // (see unregisterMBean()).
      unregisterMBean();

      final SystemConfiguration mbean = new SystemConfiguration(localHost);
      try {
         mbs.registerMBean(mbean, name);
      } catch (InstanceAlreadyExistsException e) {
         LOG.error("An instance of the system configuration mbean already exists.  If you want to update it unregister and register again.", e);
      } catch (MBeanRegistrationException e) {
         LOG.error("There was a problem registering the system configuration mbean.", e);
      } catch (NotCompliantMBeanException e) {
         LOG.error("The system configuration mbean is not compliant.  Please check the mbean for compliance.", e);
      }
   }

   public void unregisterMBean() {
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

      try {
         mbs.unregisterMBean(name);
      } catch (InstanceNotFoundException e) {
         LOG.info("No worries.  Just to let you know, the system configuration mbean is already in an unregistered state.");
      } catch (MBeanRegistrationException e) {
         LOG.error("There was a problem unregistering the system configuration mbean.", e);
      }
   }
}
