package com.rackspace.papi.jmx;

import com.rackspace.papi.service.reporting.jmx.*;

import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import java.util.ArrayList;
import java.util.List;
import javax.management.openmbean.CompositeType;

public class ConfigurationInformationCompositeDataBuilder extends CompositeDataBuilder {

    private final ConfigurationInformation.FilterInformation filter;
    private CompositeType compositeType;

    public ConfigurationInformationCompositeDataBuilder(ConfigurationInformation.FilterInformation filter) {
        this.filter = filter;
    }

    @Override
    public String getItemName() {
        return filter.getName();
    }

    @Override
    public String getDescription() {
        return "Information about filter " + filter.getName() + ".";
    }

    @Override
    public String[] getItemNames() {
        return new String[]{"id", "name", "regex", "configuration","successfully initialized","successfully loaded configurations","loading failed configurations"};
    }

    @Override
    public String[] getItemDescriptions() {
        return new String[]{"The filter id in the filter chain.",
                "The filter name.",
                "The URI Reg Ex for this filter.",
                "The configuration file specified for this filter instance.",
                "Boolean indication of configuration loaded correctly or not.",
                "successfully loaded configuration files",
                "Failed loading configuration files"
              };
    }

    @Override
    public OpenType[] getItemTypes() {
        return new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING,SimpleType.BOOLEAN, SimpleType.STRING, SimpleType.STRING};
    }

    @Override
    public Object[] getItems() {
        final List<Object> items = new ArrayList<Object>();

        items.add(filter.getId());
        items.add(filter.getName());
        items.add(filter.getRegex());
        items.add(filter.getConfiguration());
        items.add(filter.getIsConfiguarationLoaded());
        StringBuilder SuccessConfigurationLoading=new StringBuilder();
        for (String key : filter.getSuccessConfigurationLoadinginformation().keySet()){
            String[] successInformation=filter.getSuccessConfigurationLoadinginformation().get(key);
            SuccessConfigurationLoading.append(key+": ");
            SuccessConfigurationLoading.append(successInformation[0]+": ");
            SuccessConfigurationLoading.append(successInformation[1]);
            SuccessConfigurationLoading.append(System.getProperty("line.separator"));
        }
        
        items.add(SuccessConfigurationLoading.toString());
        
         StringBuilder FailedConfigurationLoading=new StringBuilder();;
        for (String key : filter.getFailedConfigurationLoadingInformation().keySet()){
            String[] successInformation=filter.getFailedConfigurationLoadingInformation().get(key);
            FailedConfigurationLoading.append(key+": ");
            FailedConfigurationLoading.append(successInformation[0]+": ");
            FailedConfigurationLoading.append(successInformation[1]+": ");
            FailedConfigurationLoading.append(successInformation[2]);
            FailedConfigurationLoading.append(System.getProperty("line.separator"));
           
        }
        items.add( FailedConfigurationLoading.toString());
        return items.toArray();
    }
    

}
