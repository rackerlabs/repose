package com.rackspace.papi.jmx;

import com.rackspace.papi.service.reporting.jmx.*;

import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationInformationCompositeDataBuilder extends CompositeDataBuilder {

    private final ConfigurationInformation.FilterInformation filter;

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
        return new String[]{"id", "name", "regex", "configuration"};
    }

    @Override
    public String[] getItemDescriptions() {
        return new String[]{"The filter id in the filter chain.",
                "The filter name.",
                "The URI Reg Ex for this filter.",
                "The configuration file specified for this filter instance."};
    }

    @Override
    public OpenType[] getItemTypes() {
        return new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING};
    }

    @Override
    public Object[] getItems() {
        final List<Object> items = new ArrayList<Object>();

        items.add(filter.getId());
        items.add(filter.getName());
        items.add(filter.getRegex());
        items.add(filter.getConfiguration());

        return items.toArray();
    }
}
