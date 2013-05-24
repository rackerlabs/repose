package com.rackspace.papi.jmx;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.digest.impl.SHA1MessageDigester;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.model.Filter;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import javax.servlet.ServletContextEvent;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.util.*;

@Component("reposeConfigurationInformation")
@ManagedResource(objectName = "com.rackspace.papi.jmx:type=ConfigurationInformation", description = "Repose configuration information MBean.")
public class ConfigurationInformation implements ConfigurationInformationMBean, ServletContextAware {

   private static final Logger LOG = LoggerFactory.getLogger(ConfigurationInformation.class);
   private final ConfigurationService configurationService;
   private ServicePorts ports;
   private final List<FilterInformation> filterChain;
   private SystemModelListener systemModelListener;
   private static final String filterError = "Error updating Mbean for Filter";

   public static class FilterInformation {

      private final String id;
      private final String name;
      private final String regex;
      private final String configuration;
      private boolean isConfiguarationLoaded;
      private Map successConfigurationLoadinginformation;
      private Map failedConfigurationLoadingInformation;

      public FilterInformation(String id, String name, String regex, String configuration, Boolean isConfiguarationLoaded) {
         this.id = id;
         this.name = name;
         this.regex = regex;
         this.configuration = configuration;
         this.isConfiguarationLoaded = isConfiguarationLoaded;
         successConfigurationLoadinginformation = new HashMap<String, String[]>();
         failedConfigurationLoadingInformation = new HashMap<String, String[]>();


      }

      public String getId() {
         return id;
      }

      public String getName() {
         return name;
      }

      public String getRegex() {
         return regex;
      }

      public String getConfiguration() {
         return configuration;
      }

      public Boolean getIsConfiguarationLoaded() {
         return isConfiguarationLoaded;
      }

      public void setConfiguarationLoaded(boolean isConfiguarationLoaded) {
         this.isConfiguarationLoaded = isConfiguarationLoaded;
      }

      public Map<String, String[]> getSuccessConfigurationLoadinginformation() {
         return successConfigurationLoadinginformation;
      }

      public void setSuccessConfigurationLoadinginformation(Map successConfigurationLoadinginformation) {
         this.successConfigurationLoadinginformation = successConfigurationLoadinginformation;
      }

      public Map<String, String[]> getFailedConfigurationLoadingInformation() {
         return failedConfigurationLoadingInformation;
      }

      public void setFailedConfigurationLoadingInformation(Map failedConfigurationLoadingInformation) {
         this.failedConfigurationLoadingInformation = failedConfigurationLoadingInformation;
      }
   }

   private static class SystemModelListener implements UpdateListener<SystemModel> {

      private boolean initialized = false;
      private final List<FilterInformation> filters;
      private final ServicePorts ports;

      SystemModelListener(List<FilterInformation> filters, ServicePorts ports) {
         this.filters = filters;
         this.ports = ports;
      }

      @Override
      public void configurationUpdated(SystemModel systemModel) {
         LOG.info("System model updated");
         initialized = false;

         SystemModelInterrogator interrogator = new SystemModelInterrogator(ports);
         ReposeCluster cluster = interrogator.getLocalServiceDomain(systemModel);

         synchronized (filters) {
            filters.clear();

             if (cluster.getFilters() != null && cluster.getFilters().getFilter() != null) {
                 for (Filter filter : cluster.getFilters().getFilter()) {
                     filters.add(new FilterInformation(filter.getId(), filter.getName(), filter.getUriRegex(),
                                                       filter.getConfiguration(), false));
                 }
             }
         }

         initialized = true;
      }

      @Override
      public boolean isInitialized() {
         return initialized;
      }
   }

   @Autowired
   public ConfigurationInformation(@Qualifier("configurationManager") ConfigurationService configurationService, @Qualifier("servicePorts") ServicePorts ports) {
      filterChain = new ArrayList<FilterInformation>();
      this.configurationService = configurationService;
      this.ports = ports;
   }

   @Override
   @ManagedOperation
   public List<CompositeData> getFilterChain() throws OpenDataException {
      List<CompositeData> list = new ArrayList<CompositeData>();
      synchronized (filterChain) {
         for (FilterInformation filter : filterChain) {
            list.add(new ConfigurationInformationCompositeDataBuilder(filter).toCompositeData());
         }
      }

      return list;
   }

   @Override
   public void contextInitialized(ServletContextEvent sce) {
      systemModelListener = new SystemModelListener(filterChain, ports);
      configurationService.subscribeTo("", "system-model.cfg.xml", systemModelListener, SystemModel.class);
   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
      configurationService.unsubscribeFrom("system-model.cfg.xml", systemModelListener);
      systemModelListener = null;
   }

   public void setFilterLoadingInformation(String filterName, boolean filterInitialized, ConfigurationResource configurationResource) {


      synchronized (filterChain) {
         for (FilterInformation filter : filterChain) {
            if (filterName.equalsIgnoreCase(filter.getName())) {
               filter.setConfiguarationLoaded(filterInitialized);
               try {
                  GregorianCalendar gcal = new GregorianCalendar();
                  XMLGregorianCalendar xgcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);

                  try {
                     if (configurationResource != null) {



                        filter.successConfigurationLoadinginformation.put(configurationResource.name(), new String[]{xgcal.toString(), byteArrayToHexString(new SHA1MessageDigester().digestStream(configurationResource.newInputStream()))});
                        if (filter.failedConfigurationLoadingInformation.containsKey(configurationResource.name())) {
                           filter.failedConfigurationLoadingInformation.remove(configurationResource.name());
                        } else if (configurationService.getResourceResolver().resolve(filter.getConfiguration()).name().equalsIgnoreCase(configurationResource.name())) {
                           filter.failedConfigurationLoadingInformation.clear();
                        }


                     }

                  } catch (IOException e) {
                     filter.failedConfigurationLoadingInformation.put(configurationResource.name(), new String[]{xgcal.toString(), "", e.getMessage()});
                     LOG.debug(filterError, e);

                  }
               } catch (Exception e) {
                  LOG.debug(filterError, e);
               }
            }
         }
      }
   }

   public void setFilterLoadingFailedInformation(String filterName, ConfigurationResource configurationResource, String errorInformation) {

      synchronized (filterChain) {
         for (FilterInformation filter : filterChain) {
            if (filterName.equalsIgnoreCase(filter.getName())) {
               try {
                  GregorianCalendar gcal = new GregorianCalendar();
                  XMLGregorianCalendar xgcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
                  try {
                     xgcal.getTimezone();

                     if (configurationResource != null) {
                        if (filter.failedConfigurationLoadingInformation.containsKey(configurationResource.name())) {
                           filter.failedConfigurationLoadingInformation.remove(configurationResource.name());
                        }
                        filter.failedConfigurationLoadingInformation.put(configurationResource.name(), new String[]{xgcal.toString(), byteArrayToHexString(new SHA1MessageDigester().digestStream(configurationResource.newInputStream())), errorInformation});

                     }

                  } catch (IOException e) {
                     filter.failedConfigurationLoadingInformation.put(configurationResource.name(), new String[]{xgcal.toString(), "", e.getMessage()});
                     LOG.debug(filterError, e);

                  }
               } catch (Exception e) {
                  LOG.debug(filterError, e);
               }
            }
         }
      }
   }

   public static String byteArrayToHexString(byte[] b) {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < b.length; i++) {
         builder.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));
      }
      return builder.toString();
   }
}
