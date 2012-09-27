package com.rackspace.papi.mocks;

import javax.xml.datatype.DatatypeConfigurationException;

/**
 * This resource should be the parent class of every mock resource defined in
 * this library. Extending this class by extending a child of this class is in
 * line with this requirement (you may have nested inheritance models).
 */
public class BaseResource {

    private final DataProvider provider;

    public BaseResource() throws DatatypeConfigurationException {
       this(new DataProviderImpl());
    }
    
    public BaseResource(DataProvider provider) throws DatatypeConfigurationException {
        this.provider = provider;
    }
    
    protected <P extends DataProvider> P getProvider() {
       return (P) provider;
    }
}
