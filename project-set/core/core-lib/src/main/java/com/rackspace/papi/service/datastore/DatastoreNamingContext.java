package com.rackspace.papi.service.datastore;

import com.rackspace.papi.service.naming.PowerApiNamingContext;

import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import java.util.Hashtable;

public class DatastoreNamingContext extends PowerApiNamingContext {

    private final DatastoreManager datastoreManager;
    
    public DatastoreNamingContext(String contextName, Hashtable environment, DatastoreManager datastoreManager) {
        super(contextName, environment);
        
        this.datastoreManager = datastoreManager;
    }

    @Override
    public Object lookup(Name name) throws NamingException {
        final Object lookedUpObject = datastoreManager.getDatastore(name.toString());
        
        if (lookedUpObject == null) {
            throw new NameNotFoundException();
        }
        
        return lookedUpObject;
    }

    @Override
    protected void validateBindingObject(Object binding) throws NamingException {
        throw new OperationNotSupportedException();
    }
}
