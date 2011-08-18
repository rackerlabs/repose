package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.cluster.ClusterView;
import com.rackspace.papi.service.datastore.impl.AbstractMapDatastoreManager;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HashRingDatastoreManager extends AbstractMapDatastoreManager<HashRingDatastore> {

    private static final Logger LOG = LoggerFactory.getLogger(HashRingDatastoreManager.class);
    public static final String DATASTORE_MANAGER_NAME = "distributed/hash-ring";
    
    private final Datastore localDatastore;
    private final String hostKey;
    private final ClusterView clusterView;
    private final HttpClient httpClientInstance;
    
    public HashRingDatastoreManager(String hostKey, ClusterView clusterView, Datastore localDatastore) {
        super(new HashMap<String, HashRingDatastore>());
        
        this.hostKey = hostKey;
        this.localDatastore = localDatastore;
        this.clusterView = clusterView;
        
        final SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        
        httpClientInstance = new DefaultHttpClient(new ThreadSafeClientConnManager(registry));
    }

    @Override
    protected HashRingDatastore newDatastore(String key) {
        try {
            final HashRingDatastore datastore = new MD5HashRingDatastore(key, clusterView, localDatastore);
            datastore.setHttpClient(httpClientInstance);
            datastore.setHostKey(hostKey);
            
            return datastore;
        } catch(NoSuchAlgorithmException algorithmException) {
            LOG.error("Unable to create hash-ring datastore. Hashing algorithm is missing. Reason: " + algorithmException.getMessage(), algorithmException);
            
            throw new DatastoreOperationException("Unable to create hash-ring datastore. Hashing algorithm is missing. Reason: " + algorithmException.getMessage(), algorithmException);
        }
    }

    @Override
    public boolean isDistributed() {
        return true;
    }
}