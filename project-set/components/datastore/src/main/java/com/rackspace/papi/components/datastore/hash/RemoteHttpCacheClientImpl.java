package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.commons.util.http.ExtendedHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import com.rackspace.papi.commons.util.pooling.ConstructionStrategy;
import com.rackspace.papi.commons.util.pooling.GenericBlockingResourcePool;
import com.rackspace.papi.commons.util.pooling.Pool;
import com.rackspace.papi.commons.util.pooling.ResourceConstructionException;
import com.rackspace.papi.commons.util.pooling.ResourceContext;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import com.rackspace.papi.commons.util.pooling.SimpleResourceContext;
import com.rackspace.papi.components.datastore.CacheRequest;
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
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteHttpCacheClientImpl implements RemoteCacheClient {

   private static final Logger LOG = LoggerFactory.getLogger(RemoteHttpCacheClientImpl.class);
   private final Pool<HttpClient> httpClientPool;
   private String hostKey;

   public RemoteHttpCacheClientImpl(final int connectionTimeout, final int socketTimeout) {
      httpClientPool = new GenericBlockingResourcePool<HttpClient>(new ConstructionStrategy<HttpClient>() {

         @Override
         public HttpClient construct() throws ResourceConstructionException {
            final HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout);
            HttpConnectionParams.setSoTimeout(httpParams, socketTimeout);

            final HttpClient newClient = new DefaultHttpClient(httpParams);

            return newClient;
         }
      }, 1, 15);
   }

   public void setHostKey(String hostKey) {
      if (hostKey == null) {
         throw new IllegalArgumentException();
      }

      this.hostKey = hostKey;
   }

   @Override
   public StoredElement get(final String key, InetSocketAddress remoteEndpoint) throws RemoteConnectionException {
      final String targetUrl = CacheRequest.urlFor(remoteEndpoint, key);

      final HttpGet cacheObjectGet = new HttpGet(targetUrl);
      cacheObjectGet.setHeader(DatastoreRequestHeaders.DATASTORE_HOST_KEY, hostKey);

      return httpClientPool.use(new ResourceContext<HttpClient, StoredElement>() {

         @Override
         public StoredElement perform(HttpClient client) throws ResourceConstructionException {

            HttpEntity responseEnttiy = null;

            try {
               final HttpResponse response = client.execute(cacheObjectGet);
               final int statusCode = response.getStatusLine().getStatusCode();

               responseEnttiy = response.getEntity();

               if (statusCode == HttpStatusCode.OK.intValue()) {
                  final InputStream internalStreamReference = response.getEntity().getContent();

                  return new StoredElementImpl(key, RawInputStreamReader.instance().readFully(internalStreamReference));
               } else if (statusCode != HttpStatusCode.NOT_FOUND.intValue()) {
                  throw new DatastoreOperationException("Remote request failed with: " + statusCode);
               }
            } catch (IOException ioe) {
               throw new RemoteConnectionException("Unable to perform put against target: " + targetUrl, ioe);
            } finally {
               releaseEntity(responseEnttiy);
            }

            return new StoredElementImpl(key, null);
         }
      });
   }

   @Override
   public boolean delete(String key, InetSocketAddress remoteEndpoint) throws RemoteConnectionException {
      final String targetUrl = CacheRequest.urlFor(remoteEndpoint, key);

      final HttpDelete cacheObjectDelete = new HttpDelete(targetUrl);
      cacheObjectDelete.addHeader(DatastoreRequestHeaders.DATASTORE_HOST_KEY, hostKey);

      return httpClientPool.use(new ResourceContext<HttpClient, Boolean>() {

         @Override
         public Boolean perform(HttpClient client) throws ResourceContextException {
            HttpEntity responseEnttiy = null;

            try {
               final HttpResponse response = client.execute(cacheObjectDelete);
               responseEnttiy = response.getEntity();

               return response.getStatusLine().getStatusCode() == HttpStatusCode.ACCEPTED.intValue();
            } catch (IOException ioe) {
               throw new RemoteConnectionException("Unable to perform put against target: " + targetUrl, ioe);
            } finally {
               releaseEntity(responseEnttiy);
            }
         }
      });
   }

   @Override
   public void put(String key, final byte[] value, int ttl, TimeUnit timeUnit, InetSocketAddress remoteEndpoint) throws RemoteConnectionException {
      final String targetUrl = CacheRequest.urlFor(remoteEndpoint, key);

      final HttpPut cacheObjectPut = new HttpPut(targetUrl);
      cacheObjectPut.addHeader(DatastoreRequestHeaders.DATASTORE_HOST_KEY, hostKey);
      cacheObjectPut.addHeader(ExtendedHttpHeader.X_TTL.getHeaderKey(), String.valueOf(TimeUnit.SECONDS.convert(ttl, timeUnit)));

      httpClientPool.use(new SimpleResourceContext<HttpClient>() {

         @Override
         public void perform(HttpClient client) throws ResourceContextException {
            HttpEntity responseEnttiy = null;

            try {
               cacheObjectPut.setEntity(new ByteArrayEntity(value));

               final HttpResponse response = client.execute(cacheObjectPut);
               responseEnttiy = response.getEntity();

               if (response.getStatusLine().getStatusCode() != HttpStatusCode.ACCEPTED.intValue()) {
                  throw new DatastoreOperationException("Remote request failed with: " + response.getStatusLine().getStatusCode());
               }
            } catch (IOException ioe) {
               throw new RemoteConnectionException("Unable to perform put against target: " + targetUrl, ioe);
            } finally {
               releaseEntity(responseEnttiy);
            }
         }
      });
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
