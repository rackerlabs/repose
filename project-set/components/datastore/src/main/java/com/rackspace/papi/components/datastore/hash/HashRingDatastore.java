package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import com.rackspace.papi.service.datastore.impl.CoalescentDatastoreWrapper;
import com.rackspace.papi.service.datastore.cluster.ClusterView;
import com.rackspace.papi.components.datastore.DatastoreRequestHeaders;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.HashedDatastore;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.impl.StoredElementImpl;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HashRingDatastore extends CoalescentDatastoreWrapper implements HashedDatastore {

    private static final Logger LOG = LoggerFactory.getLogger(HashRingDatastore.class);
    private final ClusterView clusterView;
    private final String datastorePrefix;
    private HttpClient httpClient;
    private String hostKey;

    public HashRingDatastore(String datastorePrefix, ClusterView clusterView, Datastore localDatastore) {
        super(localDatastore);

        this.datastorePrefix = datastorePrefix;
        this.clusterView = clusterView;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setHostKey(String hostKey) {
        if (hostKey == null) {
            throw new IllegalArgumentException();
        }

        this.hostKey = hostKey;
    }

    protected abstract BigInteger maxValue();

    protected abstract byte[] hash(String key);

    private byte[] getHash(String key) {
        return hash(datastorePrefix + key);
    }

    public InetSocketAddress getTarget(byte[] hashBytes) {
        final InetSocketAddress[] ringMembers = clusterView.members();

        if (ringMembers.length <= 0) {
            return clusterView.local();
        }

        final BigInteger ringSliceSize = maxValue().divide(BigInteger.valueOf(ringMembers.length));
        final int memberAddress = new BigInteger(hashBytes).divide(ringSliceSize).abs().intValue();

        if (memberAddress > ringMembers.length) {
            throw new UnaddressableKeyException("Unable to address given key");
        }

        return ringMembers[memberAddress];
    }

    @Override
    public StoredElement get(String key) throws DatastoreOperationException {
        final byte[] keyHash = getHash(key);

        return get(Base64.encodeBase64URLSafeString(keyHash), keyHash);
    }

    @Override
    public StoredElement getByHash(String base64EncodedHashString) throws DatastoreOperationException {
        return get(base64EncodedHashString, Base64.decodeBase64(base64EncodedHashString));
    }

    public StoredElement get(String name, byte[] id) throws DatastoreOperationException {
        final InetSocketAddress target = getTarget(id);

        if (!target.equals(clusterView.local())) {
            LOG.debug(clusterView.local().toString() + ":: Routing datastore get request for, \"" + name + "\" to: " + target.toString());

            return remoteGet(name, target);
        }

        return super.get(name);
    }

    @Override
    public void put(String key, byte[] value) throws DatastoreOperationException {
        put(key, value, 3, TimeUnit.MINUTES);
    }

    @Override
    public void put(String key, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
        final byte[] keyHash = getHash(key);

        put(Base64.encodeBase64URLSafeString(keyHash), keyHash, value, ttl, timeUnit);
    }

    @Override
    public void putByHash(String base64EncodedHashString, byte[] value) {
        put(base64EncodedHashString, Base64.decodeBase64(hostKey), value, 3, TimeUnit.MINUTES);
    }

    @Override
    public void putByHash(String base64EncodedHashString, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
        put(base64EncodedHashString, Base64.decodeBase64(base64EncodedHashString), value, ttl, timeUnit);
    }

    public void put(String name, byte[] id, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
        final InetSocketAddress target = getTarget(id);

        if (!target.equals(clusterView.local())) {
            LOG.debug(clusterView.local().toString() + ":: Routing datastore put request for, \"" + name + "\" to: " + target.toString());

            remotePut(name, value, ttl, timeUnit, target);
        } else {
            super.put(name, value, ttl, timeUnit);
        }
    }

    public String urlFor(InetSocketAddress remoteEndpoint, String key) {
        return new StringBuilder("http://").append(remoteEndpoint.getAddress().getHostName()).append(":").append(remoteEndpoint.getPort()).append("/powerapi/datastore/objects/").append(key).toString();
    }

    public StoredElement remoteGet(String key, InetSocketAddress remoteEndpoint) {
        final String targetUrl = urlFor(remoteEndpoint, key);

        final HttpGet cacheObjectGet = new HttpGet(targetUrl);
        cacheObjectGet.setHeader(DatastoreRequestHeaders.DATASTORE_HOST_KEY, hostKey);

        HttpEntity responseEnttiy = null;

        try {
            final HttpResponse response = httpClient.execute(cacheObjectGet);
            final int statusCode = response.getStatusLine().getStatusCode();

            responseEnttiy = response.getEntity();

            if (statusCode == HttpStatusCode.OK.intValue()) {
                final InputStream internalStreamReference = response.getEntity().getContent();

                return new StoredElementImpl(key, RawInputStreamReader.instance().readFully(internalStreamReference));
            } else if (statusCode != HttpStatusCode.NOT_FOUND.intValue()) {
                throw new DatastoreOperationException("Remote request failed with: " + statusCode);
            }
        } catch (Exception ex) {
            throw new DatastoreOperationException("Get failed for: " + targetUrl + " - reason: " + ex.getMessage(), ex);
        } finally {
            releaseEntity(responseEnttiy);
        }

        return new StoredElementImpl(key, null);
    }

    public void remotePut(String key, byte[] value, int ttl, TimeUnit timeUnit, InetSocketAddress remoteEndpoint) {
        final String targetUrl = urlFor(remoteEndpoint, key);

        final HttpPut cacheObjectPut = new HttpPut(targetUrl);
        cacheObjectPut.addHeader(DatastoreRequestHeaders.DATASTORE_HOST_KEY, hostKey);
        cacheObjectPut.addHeader(DatastoreRequestHeaders.DATASTORE_TTL, String.valueOf(TimeUnit.SECONDS.convert(ttl, timeUnit)));

        HttpEntity responseEnttiy = null;

        try {
            cacheObjectPut.setEntity(new ByteArrayEntity(value));

            final HttpResponse response = httpClient.execute(cacheObjectPut);
            responseEnttiy = response.getEntity();

            if (response.getStatusLine().getStatusCode() != HttpStatusCode.ACCEPTED.intValue()) {
                throw new DatastoreOperationException("Remote request failed with: " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception ex) {
            throw new DatastoreOperationException("Get failed for: " + targetUrl + " - reason: " + ex.getMessage(), ex);
        } finally {
            releaseEntity(responseEnttiy);
        }
    }

    private void releaseEntity(HttpEntity e) {
        if (e != null) {
            try {
                EntityUtils.consume(e);
            } catch (IOException ex) {
                LOG.error("Failure in releasing HTTPClient resources. Reason: " + ex.getMessage(), ex);
            }
        }
    }
}
