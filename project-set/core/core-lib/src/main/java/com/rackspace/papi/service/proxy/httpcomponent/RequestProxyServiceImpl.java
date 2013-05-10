package com.rackspace.papi.service.proxy.httpcomponent;

import com.rackspace.papi.commons.util.StringUriUtilities;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.http.proxy.HttpException;
import com.rackspace.papi.commons.util.proxy.ProxyUtilities;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("apacheRequestProxyService")
public class RequestProxyServiceImpl implements RequestProxyService {

   private static final Logger LOG = LoggerFactory.getLogger(RequestProxyServiceImpl.class);
   private static final int DEFAULT_HTTPS_PORT = 443;
   private final Object clientLock = new Object();
   private Integer connectionTimeout = Integer.valueOf(0);
   private Integer readTimeout = Integer.valueOf(0);
   private PoolingClientConnectionManager manager;
   private DefaultHttpClient client;
   private Integer proxyThreadPool;
   private boolean rewriteHostHeader = false;

   private HttpHost getProxiedHost(String targetHost) throws HttpException {
      try {
         return URIUtils.extractHost(new URI(targetHost));
      } catch (URISyntaxException ex) {
         LOG.error("Invalid target host url: " + targetHost, ex);
      }

      throw new HttpException("Invalid target host");
   }

   private HttpClient getClient() {
      synchronized (clientLock) {
         if (client == null) {
            LOG.info("Building Apache Components Http v4 Client");
            manager = new PoolingClientConnectionManager();
            manager.setMaxTotal(proxyThreadPool);
            manager.setDefaultMaxPerRoute(proxyThreadPool);
            SSLContext sslContext = ProxyUtilities.getTrustingSslContext();
            SSLSocketFactory ssf = new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            SchemeRegistry registry = manager.getSchemeRegistry();
            Scheme scheme = new Scheme("https", DEFAULT_HTTPS_PORT, ssf);
            registry.register(scheme);
            client = new DefaultHttpClient(manager);
            client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
            client.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, readTimeout);
            client.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
         }

         return client;
      }

   }

   @Override
   public int proxyRequest(String targetHost, HttpServletRequest request, HttpServletResponse response) throws IOException {
      try {

         final HttpHost proxiedHost = getProxiedHost(targetHost);
         final String target = proxiedHost.toURI() + request.getRequestURI();
         final HttpComponentRequestProcessor processor = new HttpComponentRequestProcessor(request, new URI(proxiedHost.toURI()), rewriteHostHeader);
         final HttpComponentProcessableRequest method = HttpComponentFactory.getMethod(request.getMethod(), processor.getUri(target));
         ((MutableHttpServletRequest)request).removeHeader("Content-Length");

         if (method != null) {
            HttpRequestBase processedMethod = method.process(processor);

            return executeProxyRequest(processedMethod, response);
         }
      } catch (URISyntaxException ex) {
        LOG.error("Error processing request", ex);
      } catch (HttpException ex) {
        LOG.error("Error processing request", ex);
      }

      //Something exploded; return a status code that doesn't exist
      return -1;
   }

   private int executeProxyRequest(HttpRequestBase httpMethodProxyRequest, HttpServletResponse response) throws IOException, HttpException {

      try{
          
    
      HttpResponse httpResponse = getClient().execute(httpMethodProxyRequest);
      HttpComponentResponseCodeProcessor responseCode = new HttpComponentResponseCodeProcessor(httpResponse.getStatusLine().getStatusCode());
      HttpComponentResponseProcessor responseProcessor = new HttpComponentResponseProcessor(httpResponse, response, responseCode);

      if (responseCode.isRedirect()) {
         responseProcessor.sendTranslatedRedirect(responseCode.getCode());
      } else {
         responseProcessor.process();
      }

      return responseCode.getCode();
      } catch(ClientProtocolException ex){
          if("ReadLimitReachedException".equals(ex.getCause().getCause().getClass().getSimpleName())){
                            LOG.error("Error reading request content", ex);
                            response.sendError(HttpStatusCode.REQUEST_ENTITY_TOO_LARGE.intValue(), "Error reading request content");
            }else{
               LOG.error("Error processing request", ex);
               return -1;
          }
      }
     return 1;
  
   }

   @Override
   public void updateConfiguration(Integer connectionTimeout, Integer readTimeout, Integer proxyThreadPool, boolean requestLogging) {
      LOG.info("Updating Request Proxy configuration");
      this.connectionTimeout = connectionTimeout;
      this.readTimeout = readTimeout;
      this.proxyThreadPool = proxyThreadPool;

      // Invalidate client
      synchronized (clientLock) {
         if (manager != null) {
            manager.shutdown();
         }
         client = null;
      }
   }
   
   private void setHeaders(HttpRequestBase base, Map<String, String> headers) {
 
      final Set<Map.Entry<String, String>> entries = headers.entrySet();
      for(Map.Entry<String,String> entry : entries){
         base.addHeader(entry.getKey(), entry.getValue());
      }
   }
   
   private ServiceClientResponse execute(HttpRequestBase base) {
      try {
         HttpResponse httpResponse = getClient().execute(base);
         HttpEntity entity = httpResponse.getEntity();
         HttpComponentResponseCodeProcessor responseCode = new HttpComponentResponseCodeProcessor(httpResponse.getStatusLine().getStatusCode());

         InputStream stream = null;
         if (entity != null) {
            stream = new ByteArrayInputStream(RawInputStreamReader.instance().readFully(entity.getContent()));
            EntityUtils.consume(entity);
         }
         
         return new ServiceClientResponse(responseCode.getCode(), stream);
      } catch (IOException ex) {
         LOG.error("Error executing request", ex);
      } finally {
         base.releaseConnection();
      }
      
      return new ServiceClientResponse(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue(), null);
   }

   @Override
   public ServiceClientResponse get(String uri, Map<String, String> headers) {
      HttpGet get = new HttpGet(uri);
      setHeaders(get, headers);
      return execute(get);
   }

   @Override
   public ServiceClientResponse get(String baseUri, String extraUri, Map<String, String> headers) {
      HttpGet get = new HttpGet(StringUriUtilities.appendPath(baseUri, extraUri));
      setHeaders(get, headers);
      return execute(get);
   }

   @Override
   public ServiceClientResponse delete(String baseUri, String extraUri, Map<String, String> headers) {
      HttpDelete delete = new HttpDelete(StringUriUtilities.appendPath(baseUri, extraUri));
      setHeaders(delete, headers);
      return execute(delete);
   }

   @Override
   public ServiceClientResponse put(String uri, Map<String, String> headers, byte[] body) {
      HttpPut put = new HttpPut(uri);
      setHeaders(put, headers);
      if (body != null && body.length > 0) {
         put.setEntity(new InputStreamEntity(new ByteArrayInputStream(body), body.length));
      }
      return execute(put);
   }

   @Override
   public ServiceClientResponse put(String baseUri, String path, Map<String, String> headers, byte[] body) {
      HttpPut put = new HttpPut(StringUriUtilities.appendPath(baseUri, path));
      setHeaders(put, headers);
      if (body != null && body.length > 0) {
         put.setEntity(new InputStreamEntity(new ByteArrayInputStream(body), body.length));
      }
      return execute(put);
   }

  @Override
  public void setRewriteHostHeader(boolean value) {
    this.rewriteHostHeader = value;
  }

}
