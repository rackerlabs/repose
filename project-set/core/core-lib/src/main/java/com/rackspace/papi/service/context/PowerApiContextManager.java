package com.rackspace.papi.service.context;

import com.rackspace.papi.service.ServiceContext;
import com.rackspace.papi.service.classloader.ClassLoaderServiceContext;
import com.rackspace.papi.service.config.ConfigurationServiceContext;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.service.datastore.DatastoreServiceContext;
import com.rackspace.papi.service.deploy.ArtifactManagerServiceContext;
import com.rackspace.papi.service.event.EventManagerServiceContext;
import com.rackspace.papi.service.naming.InitialServiceContextFactory;
import com.rackspace.papi.service.thread.ThreadingServiceContext;
import com.rackspace.papi.servlet.PowerApiContextException;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class PowerApiContextManager implements ServletContextListener {
    
    private static final Logger LOG = LoggerFactory.getLogger(PowerApiContextManager.class);
    private final List<String> boundServiceContextNames;
    private Context initialContext;
    
    public PowerApiContextManager() {
        boundServiceContextNames = new LinkedList<String>();
    }
    
    private <T extends ServiceContext> T bindServletContextBoundService(T resource) {
        try {
            final String serviceName = resource.getServiceName();
            
            initialContext.bind(serviceName, resource);
            boundServiceContextNames.add(serviceName);
        } catch (NamingException ne) {
            handleNamingException("Failed to bind, \"" + resource.getServiceName() + "\" in the JNDI initial context.", ne);
        }
        
        return resource;
    }
    
    public void handleNamingException(String message, NamingException ne) throws PowerApiContextException {
        final PowerApiContextException newException = new PowerApiContextException(message + " Reason: " + ne.getExplanation(), ne);
        LOG.error(newException.getMessage(), ne);
        
        throw newException;
    }
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            //TODO:Enhancement load the initial context from a class defined as a context listener init parameter
            this.initialContext = new InitialServiceContextFactory().getInitialContext();
        } catch (NamingException ne) {
            handleNamingException("Failed to build initial context", ne);
        }
        
        try {
            // Initial subcontexts
            initialContext.createSubcontext("kernel");
            initialContext.createSubcontext("services");
        } catch (NamingException ne) {
            handleNamingException("Failed to create required subcontexts in the JNDI initial context.", ne);
        }

        // Most bootstrap steps require or will try to load some kind of
        // configuration so we need to set our naming context in the servlet context
        // first before anything else
        ServletContextHelper.setPowerApiContext(sce.getServletContext(), initialContext);

        // Services Bootstrap

        // Threading Service
        final ServiceContext threadManagerContext = bindServletContextBoundService(new ThreadingServiceContext());
        threadManagerContext.contextInitialized(sce);

        // Event kernel init
        final ServiceContext eventKernelContext = bindServletContextBoundService(new EventManagerServiceContext());
        eventKernelContext.contextInitialized(sce);

        // Configuration Services
        final ConfigurationServiceContext configurationManager = bindServletContextBoundService(new ConfigurationServiceContext());
        configurationManager.contextInitialized(sce);

        // Response message service
        final ResponseMessageServiceContext responseMessageServiceManager = bindServletContextBoundService(new ResponseMessageServiceContext());
        responseMessageServiceManager.contextInitialized(sce);

        // Datastore Service
        final DatastoreServiceContext datastoreServiceContext = bindServletContextBoundService(new DatastoreServiceContext());
        datastoreServiceContext.contextInitialized(sce);

        // Start up the class loader manager
        final ClassLoaderServiceContext clManager = bindServletContextBoundService(new ClassLoaderServiceContext());
        clManager.contextInitialized(sce);

        /// Start the deployment manager
        final ArtifactManagerServiceContext artifactManager = bindServletContextBoundService(new ArtifactManagerServiceContext());
        artifactManager.contextInitialized(sce);
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Iterator<String> iterator = ((LinkedList<String>)boundServiceContextNames).descendingIterator();
        while (iterator.hasNext()) {
            final String ctxName = iterator.next();

            try {                
                final ServiceContext ctx = (ServiceContext) initialContext.lookup(ctxName);
                initialContext.unbind(ctxName);
                
                ctx.contextDestroyed(sce);
            } catch (NamingException ne) {
                handleNamingException("Unable to destroy service context \"" + ctxName + "\" - Reason: " + ne.getMessage(), ne);
            }
        }
    }
}
