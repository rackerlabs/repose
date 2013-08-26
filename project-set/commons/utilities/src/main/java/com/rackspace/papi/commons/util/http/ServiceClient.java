package com.rackspace.papi.commons.util.http;


import com.rackspace.papi.commons.util.StringUriUtilities;
import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import com.rackspace.papi.commons.util.proxy.ProxyRequestException;
import com.rackspace.papi.service.httpclient.HttpClientNotFoundException;
import com.rackspace.papi.service.httpclient.HttpClientService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.*;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;
import java.util.Set;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.client.HttpClient;
/**
 * Creates apache http clients with basic auth
 */
public class ServiceClient {
    private static final String ACCEPT_HEADER = "Accept";
    private static final String MEDIA_TYPE = "application/xml";
    private static final Logger LOG = LoggerFactory.getLogger(ServiceClient.class);
    private static final int TIMEOUT = 30000;
    private String TargetHostUri;
    private String Username;
    private String Password;
    private String ConnectionPoolId;




    private HttpClientService httpClientService;

      public ServiceClient() {
    }

    public ServiceClient(String targetHostUri, String username, String password, String connectionPoolId) {
        TargetHostUri =  targetHostUri;
        Username  =  username;
        Password  = password;
        ConnectionPoolId= connectionPoolId;

    }

    private HttpClient getClientWithBasicAuth() throws ServiceClientException {
        try{

            final HttpClient client = httpClientService.getClient(ConnectionPoolId).getHttpClient();
            client.getParams().setParameter(AuthPNames.PROXY_AUTH_PREF, AuthPolicy.BASIC);

            CredentialsProvider credsProvider = new BasicCredentialsProvider();

            credsProvider.setCredentials(
                    new AuthScope(TargetHostUri, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(Username, Password));


            client.getParams().setParameter("http.authentication.preemptive" ,true);
            client.getParams().setParameter("http.authentication.credential-provider", credsProvider) ;

            return client;

        }catch(HttpClientNotFoundException e) {
            LOG.error("Failed to obtain an HTTP default client connection");
            throw new ServiceClientException("Failed to obtain an HTTP default client connection", e);
        }

    }

    private static class ReposeHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession sslSession) {
            LOG.info("verifying: " + hostname);
            return true;
        }
    }

    private void setHeaders(HttpRequestBase base, Map<String, String> headers) {

        final Set<Map.Entry<String, String>> entries = headers.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            base.addHeader(entry.getKey(), entry.getValue());
        }
    }

    private ServiceClientResponse execute(HttpRequestBase base,String... queryParameters) {
        try {

            HttpClient client=getClientWithBasicAuth();

            for (int index = 0; index < queryParameters.length; index = index + 2) {
               client.getParams().setParameter(queryParameters[index], queryParameters[index + 1]);
            }


            HttpResponse httpResponse = getClientWithBasicAuth().execute(base);
            HttpEntity entity = httpResponse.getEntity();
           



            InputStream stream = null;
            if (entity != null) {
                stream = new ByteArrayInputStream(RawInputStreamReader.instance().readFully(entity.getContent()));
                EntityUtils.consume(entity);
            }

            return new ServiceClientResponse(httpResponse.getStatusLine().getStatusCode(), stream);
        } catch (ServiceClientException  ex){
            LOG.error("Failed to obtain an HTTP default client connection", ex);
        }
        catch (IOException ex) {
            LOG.error("Error executing request", ex);
        } finally {
            base.releaseConnection();
        }

        return new ServiceClientResponse(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue(), null);
    }

    public ServiceClientResponse post(String uri, JAXBElement body, MediaType contentType) {
        HttpPost post = new HttpPost(uri);

        Map<String, String> headers= new HashMap<String, String>();
        String localContentType= contentType.getType() +"/"+ contentType.getSubtype();
        headers.put("Content-Type",localContentType); //test
        headers.put(ACCEPT_HEADER,MEDIA_TYPE);

        setHeaders(post, headers);

        if (body != null && !body.getValue().toString().isEmpty()) {
            post.setEntity(new InputStreamEntity(new ByteArrayInputStream(body.getValue().toString().getBytes()),body.getValue().toString().length()));
        }
        return execute(post);
    }


    public ServiceClientResponse get(String uri, Map<String, String> headers, String... queryParameters){
        HttpGet get = new HttpGet(uri);

        if (queryParameters.length % 2 != 0) {
            throw new IllegalArgumentException("Query parameters must be in pairs.");
        }

        setHeaders(get, headers);
        return execute(get,queryParameters);
    }

}
