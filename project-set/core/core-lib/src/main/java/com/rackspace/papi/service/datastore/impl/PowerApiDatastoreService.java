package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreNamingContext;
import com.rackspace.papi.service.datastore.DatastoreService;
import javax.naming.Context;
import javax.naming.NamingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PowerApiDatastoreService implements DatastoreService {

    private static final Logger LOG = LoggerFactory.getLogger(PowerApiDatastoreService.class);
    private final Context namingContext;
    private final String servicePrefix;

    public PowerApiDatastoreService(Context namingContext, String servicePrefix) {
        this.namingContext = namingContext;
        this.servicePrefix = servicePrefix;
    }

    @Override
    public Datastore defaultDatastore() {
        try {
            return (Datastore) namingContext.lookup(servicePrefix + "/" + DEFAULT_LOCAL);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public Datastore getDatastore(String datastoreName) {
        try {
            return (Datastore) namingContext.lookup(servicePrefix + datastoreName);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public void unregisterDatastoreManager(String datastoreManagerName) throws NamingException {
        final String nameInContext = servicePrefix + datastoreManagerName;
        
        try {
            namingContext.unbind(nameInContext);
        } catch (NamingException ex) {
            LOG.error(ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public void registerDatastoreManager(String datastoreManagerName, DatastoreManager manager) throws NamingException {
        final String nameInContext = servicePrefix + datastoreManagerName;
        
        try {
            namingContext.bind(nameInContext, new DatastoreNamingContext(nameInContext, namingContext.getEnvironment(), manager));
        } catch (NamingException ex) {
            LOG.error(ex.getMessage(), ex);
            throw ex;
        }
    }
}
