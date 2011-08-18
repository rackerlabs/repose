package com.rackspace.papi.service.context.jndi;

import com.rackspace.papi.service.ServiceContext;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.rms.ResponseMessageService;
import com.rackspace.papi.service.ServiceUnavailableException;
import com.rackspace.papi.service.event.EventService;
import com.rackspace.papi.service.event.EventManagerServiceContext;
import com.rackspace.papi.service.classloader.ApplicationClassLoader;
import com.rackspace.papi.service.classloader.ClassLoaderServiceContext;
import com.rackspace.papi.service.config.ConfigurationServiceContext;
import com.rackspace.papi.service.context.ResponseMessageServiceContext;
import com.rackspace.papi.service.datastore.DatastoreServiceContext;
import com.rackspace.papi.service.thread.ThreadingServiceContext;
import com.rackspace.papi.service.thread.ThreadingService;
import javax.naming.Context;
import javax.naming.NamingException;

public class JndiContextAdapter implements ContextAdapter {

    private final Context namingContext;

    public JndiContextAdapter(Context namingContext) {
        this.namingContext = namingContext;
    }

    public static <T> T lookup(String name, Context context) throws ServiceUnavailableException {
        try {
            final Object o = context.lookup(name);
            return ((ServiceContext<T>) o).getService();
        } catch (NamingException ne) {
            throw new ServiceUnavailableException(name + " service is unavailable. Reason: " + ne.getExplanation(), ne.getCause());
        }
    }

    @Override
    public ApplicationClassLoader classLoader() throws ServiceUnavailableException {
        return lookup(ClassLoaderServiceContext.SERVICE_NAME, namingContext);
    }

    @Override
    public ConfigurationService configurationService() throws ServiceUnavailableException {
        return lookup(ConfigurationServiceContext.SERVICE_NAME, namingContext);
    }

    @Override
    public DatastoreService datastoreService() throws ServiceUnavailableException {
        return lookup(DatastoreServiceContext.SERVICE_NAME, namingContext);
    }

    @Override
    public ResponseMessageService responseMessageService() throws ServiceUnavailableException {
        return lookup(ResponseMessageServiceContext.SERVICE_NAME, namingContext);
    }

    @Override
    public EventService eventService() throws ServiceUnavailableException {
        return lookup(EventManagerServiceContext.SERVICE_NAME, namingContext);
    }

    @Override
    public ThreadingService threadingService() throws ServiceUnavailableException {
        return lookup(ThreadingServiceContext.SERVICE_NAME, namingContext);

    }
}
