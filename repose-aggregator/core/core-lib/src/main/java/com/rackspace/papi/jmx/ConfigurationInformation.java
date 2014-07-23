package com.rackspace.papi.jmx;

import com.rackspace.papi.service.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.digest.impl.SHA1MessageDigester;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.service.config.ConfigurationResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

@Named
@ManagedResource(objectName = "com.rackspace.papi.jmx:type=ConfigurationInformation", description = "Repose configuration information MBean.")
public class ConfigurationInformation implements ConfigurationInformationMBean {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationInformation.class);
    private static final String FILTER_EXCEPTION_MESSAGE = "Error updating Mbean for Filter";

    private final List<FilterInformation> filterChain;
    private final ConfigurationResourceResolver resourceResolver;

    private ServicePorts ports;


    @Inject
    public ConfigurationInformation(@Qualifier("servicePorts") ServicePorts ports,
                                     ConfigurationResourceResolver resourceResolver) {
        filterChain = new ArrayList<>();
        this.resourceResolver = resourceResolver;
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

    @PostConstruct
    public void afterPropertiesSet() {
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


                                filter.getSuccessConfigurationLoadinginformation().put(configurationResource.name(), new String[]{xgcal.toString(), byteArrayToHexString(new SHA1MessageDigester().digestStream(configurationResource.newInputStream()))});
                                if (filter.getFailedConfigurationLoadingInformation().containsKey(configurationResource.name())) {
                                    filter.getFailedConfigurationLoadingInformation().remove(configurationResource.name());
                                } else if (resourceResolver.resolve(filter.getConfiguration()).name().equalsIgnoreCase(configurationResource.name())) {
                                    filter.getFailedConfigurationLoadingInformation().clear();
                                }


                            }

                        } catch (IOException e) {
                            filter.getFailedConfigurationLoadingInformation().put(configurationResource.name(), new String[]{xgcal.toString(), "", e.getMessage()});
                            LOG.debug(FILTER_EXCEPTION_MESSAGE, e);

                        }
                    } catch (Exception e) {
                        LOG.debug(FILTER_EXCEPTION_MESSAGE, e);
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
                                if (filter.getFailedConfigurationLoadingInformation().containsKey(configurationResource.name())) {
                                    filter.getFailedConfigurationLoadingInformation().remove(configurationResource.name());
                                }
                                filter.getFailedConfigurationLoadingInformation().put(configurationResource.name(), new String[]{xgcal.toString(), byteArrayToHexString(new SHA1MessageDigester().digestStream(configurationResource.newInputStream())), errorInformation});

                            }

                        } catch (IOException e) {
                            filter.getFailedConfigurationLoadingInformation().put(configurationResource.name(), new String[]{xgcal.toString(), "", e.getMessage()});
                            LOG.debug(FILTER_EXCEPTION_MESSAGE, e);

                        }
                    } catch (Exception e) {
                        LOG.debug(FILTER_EXCEPTION_MESSAGE, e);
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

    public List<FilterInformation> getFilterList() {
        return filterChain;
    }

}
