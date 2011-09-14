package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import com.rackspace.papi.components.datastore.DatastoreRequestHeaders;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.impl.StoredElementImpl;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteHttpCacheClientImpl implements RemoteCacheClient {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteHttpCacheClientImpl.class);
    
    private HttpClient httpClient;
    private String hostKey;

    public String urlFor(InetSocketAddress remoteEndpoint, String key) {
        return new StringBuilder("http://").append(remoteEndpoint.getAddress().getHostName()).append(":").append(remoteEndpoint.getPort()).append("/powerapi/datastore/objects/").append(key).toString();
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

    @Override
    public StoredElement get(String key, InetSocketAddress remoteEndpoint) {
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

    @Override
    public void put(String key, byte[] value, int ttl, TimeUnit timeUnit, InetSocketAddress remoteEndpoint) {
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
