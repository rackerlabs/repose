package com.rackspace.papi.components.datastore.hash.remote;

import com.rackspace.papi.commons.util.pooling.*;
import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class RemoteCommandExecutor {

   private static final Logger LOG = LoggerFactory.getLogger(RemoteCommandExecutor.class);
   private final Pool<HttpClient> httpClientPool;
   private String hostKey;

   public RemoteCommandExecutor(final int connectionTimeout, final int socketTimeout) {
      this(new GenericBlockingResourcePool<HttpClient>(new ConstructionStrategy<HttpClient>() {

         @Override
         public HttpClient construct() throws ResourceConstructionException {
            final HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout);
            HttpConnectionParams.setSoTimeout(httpParams, socketTimeout);

            final HttpClient newClient = new DefaultHttpClient(httpParams);

            return newClient;
         }
      }, 1, 15));
   }

   public RemoteCommandExecutor(Pool<HttpClient> httpClientPool) {
      this.httpClientPool = httpClientPool;
      this.hostKey = "";
   }
   
   public void setHostKey(String hostKey) {
      this.hostKey = hostKey;
   }

   public Object execute(final RemoteCommand command) {
      return httpClientPool.use(new ResourceContext<HttpClient, Object>() {

         @Override
         public Object perform(HttpClient resource) throws ResourceContextException {
            HttpEntity responseEntity = null;

            command.setHostKey(hostKey);
            
            try {
               final HttpRequestBase request = command.buildRequest();
               final HttpResponse response = resource.execute(request);

               responseEntity = response.getEntity();

               return command.handleResponse(response);
            } catch (IOException ioe) {
               throw new RemoteConnectionException("Unable to perform action: " + command.toString(), ioe);
            } finally {
               releaseEntity(responseEntity);
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
