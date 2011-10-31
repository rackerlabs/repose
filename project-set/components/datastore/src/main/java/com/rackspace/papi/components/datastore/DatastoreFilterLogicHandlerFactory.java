package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.datastore.hash.HashRingDatastoreManager;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.model.Filter;
import com.rackspace.papi.model.Host;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.hash.HashedDatastore;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.cluster.ThreadSafeClusterView;
import com.rackspace.papi.service.datastore.encoding.UUIDEncodingProvider;
import com.rackspace.papi.service.datastore.hash.HashProvider;
import com.rackspace.papi.service.datastore.hash.MD5HashProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DatastoreFilterLogicHandlerFactory extends AbstractConfiguredFilterHandlerFactory<DatastoreFilterLogicHandler> {

    private static final Logger LOG = LoggerFactory.getLogger(DatastoreFilterLogicHandlerFactory.class);
    private final DatastoreService datastoreService;
    private final MutableClusterView clusterView;
    private String lastLocalAddr;
    private HashRingDatastoreManager hashRingDatastoreManager;
    private HashedDatastore hashRingDatastore;

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {
         {
            put(PowerProxy.class, new SystemModelUpdateListener());
         }
      };
   }

    private class SystemModelUpdateListener implements UpdateListener<PowerProxy> {

        @Override
        public void configurationUpdated(PowerProxy configurationObject) {
            if (configurationObject == null) {
                LOG.error("Power Proxy configuration was null - please check your configurations and error logs");
                return;
            }

            try {
                final List<InetSocketAddress> cacheSiblings = new LinkedList<InetSocketAddress>();

                for (Host hostInformation : configurationObject.getHost()) {
                    if (hostInformation.getFilters() != null) {
                        for (Filter f : hostInformation.getFilters().getFilter()) {
                            if (f.getName().equals("dist-datastore")) {
                                final InetAddress hostAddress = InetAddress.getByName(hostInformation.getHostname());
                                final InetSocketAddress hostSocketAddress = new InetSocketAddress(hostAddress, hostInformation.getServicePort());

                                cacheSiblings.add(hostSocketAddress);
                            }
                        }
                    }
                }

                clusterView.updateMembers(cacheSiblings.toArray(new InetSocketAddress[cacheSiblings.size()]));
            } catch (UnknownHostException uhe) {
                LOG.error(uhe.getMessage(), uhe);
            }

            if (hashRingDatastoreManager == null) {
                try {
                    final HashProvider hashProvider = new MD5HashProvider();

                    hashRingDatastoreManager = new HashRingDatastoreManager("temp-host-key", UUIDEncodingProvider.getInstance(), hashProvider, clusterView, datastoreService.defaultDatastore());
                    hashRingDatastore = hashRingDatastoreManager.newDatastoreServer("default");

                    datastoreService.registerDatastoreManager(HashRingDatastoreManager.DATASTORE_MANAGER_NAME, hashRingDatastoreManager);
                } catch (NoSuchAlgorithmException algorithmException) {
                    LOG.error("Unable to create hash-ring datastore. Hashing algorithm is missing. Reason: " + algorithmException.getMessage(), algorithmException);

                    throw new DatastoreOperationException("Unable to create hash-ring datastore. Hashing algorithm is missing. Reason: " + algorithmException.getMessage(), algorithmException);
                } catch (NamingException ne) {
                    LOG.error(ne.getExplanation(), ne);
                }
            }
        }
    }
    
   @Override
   protected DatastoreFilterLogicHandler buildHandler() {
      return new DatastoreFilterLogicHandler(datastoreService, clusterView, lastLocalAddr, hashRingDatastore);
   }
    

    public DatastoreFilterLogicHandlerFactory(DatastoreService ds) {
        datastoreService = ds;
        clusterView = new ThreadSafeClusterView();
    }
}
