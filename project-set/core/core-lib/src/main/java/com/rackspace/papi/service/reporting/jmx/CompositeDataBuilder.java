package com.rackspace.papi.service.reporting.jmx;

import javax.management.openmbean.*;

public abstract class CompositeDataBuilder {

    public abstract String getItemName();

    public abstract String getDescription();

    public abstract String[] getItemNames();

    public abstract String[] getItemDescriptions();

    public abstract OpenType[] getItemTypes();

    public abstract Object[] getItems();

    public CompositeData toCompositeData() throws OpenDataException {
        return new CompositeDataSupport(getCompositeType(), getItemNames(), getItems());
    }

    private CompositeType getCompositeType() throws OpenDataException {
        return new CompositeType(getItemName(), getDescription(), getItemNames(), getItemDescriptions(), getItemTypes());
    }
}
