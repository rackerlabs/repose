package org.openrepose.commons.utils.http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.io.RawInputStreamReader;
import org.openrepose.services.httpclient.HttpClientNotFoundException;
import org.openrepose.services.httpclient.HttpClientResponse;
import org.openrepose.services.httpclient.HttpClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Creates apache http clients with basic auth
 */
public class ServiceClient {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceClient.class);
    private String targetHostUri;
    private String username;
    private String password;
    private String connectionPoolId;

    private HttpClientService httpClientService;

    public ServiceClient(String connectionPoolId,HttpClientService httpClientService) {
        this.connectionPoolId= connectionPoolId;
        this.httpClientService=httpClientService;
    }

    public ServiceClient(String targetHostUri, String username, String password, String connectionPoolId,HttpClientService httpClientService) {
        this.targetHostUri =  targetHostUri;
        this.username  =  username;
        this.password  = password;
        this.connectionPoolId= connectionPoolId;
        this.httpClientService=httpClientService;

    }

    private HttpClient getClientWithBasicAuth() throws ServiceClientException {
        HttpClientResponse clientResponse = null;

        try{

            clientResponse = httpClientService.getClient(connectionPoolId);
            final HttpClient client = clientResponse.getHttpClient();

            if(!StringUtilities.isEmpty(targetHostUri) && !StringUtilities.isEmpty(username) && !StringUtilities.isEmpty(password) )  {

                client.getParams().setParameter(AuthPNames.PROXY_AUTH_PREF, AuthPolicy.BASIC);

                CredentialsProvider credsProvider = new BasicCredentialsProvider();

                credsProvider.setCredentials(
                        new AuthScope(targetHostUri, AuthScope.ANY_PORT),
                        new UsernamePasswordCredentials(username, password));
                client.getParams().setParameter("http.authentication.credential-provider", credsProvider) ;

            }

            return client;

        } catch(HttpClientNotFoundException e) {
            LOG.error("Failed to obtain an HTTP default client connection");
            throw new ServiceClientException("Failed to obtain an HTTP default client connection", e);
        } finally {
            if (clientResponse != null) {
                httpClientService.releaseClient(clientResponse);
            }
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

            HttpResponse httpResponse = client.execute(base);
            HttpEntity entity = httpResponse.getEntity();

            InputStream stream = null;
            if (entity != null) {
                stream = new ByteArrayInputStream(RawInputStreamReader.instance().readFully(entity.getContent()));
                EntityUtils.consume(entity);
            }

            return new ServiceClientResponse(httpResponse.getStatusLine().getStatusCode(), httpResponse.getAllHeaders(), stream);
        } catch (ServiceClientException  ex){
            LOG.error("Failed to obtain an HTTP default client connection", ex);
        } catch (IOException ex) {
            LOG.error("Error executing request", ex);
        } finally {
            base.releaseConnection();

        }

        return new ServiceClientResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
    }

    public ServiceClientResponse post(String uri, String body, MediaType contentMediaType) {
        return post(uri, new HashMap<String, String>(), body, contentMediaType);
    }

    public ServiceClientResponse post(String uri, Map<String, String> headers, String body, MediaType contentMediaType) {
        HttpPost post = new HttpPost(uri);

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.putAll(headers);
        String localContentType= contentMediaType.getType() +"/"+ contentMediaType.getSubtype();
        requestHeaders.put(CommonHttpHeader.CONTENT_TYPE.toString(), localContentType);

        // TODO: Remove setting the accept type to XML by default
        if (!requestHeaders.containsKey(CommonHttpHeader.ACCEPT.toString())) {
            requestHeaders.put(CommonHttpHeader.ACCEPT.toString(), MediaType.APPLICATION_XML);
        }

        setHeaders(post, requestHeaders);

        if (body != null && !body.isEmpty()) {
            post.setEntity(new InputStreamEntity(new ByteArrayInputStream(body.getBytes()),body.length()));
        }
        return execute(post);
    }

    public ServiceClientResponse get(String uri, Map<String, String> headers, String... queryParameters){

        URI uriBuilt;
        HttpGet httpget = new HttpGet(uri);

        if (queryParameters != null) {

            if (queryParameters.length % 2 != 0) {
                throw new IllegalArgumentException("Query parameters must be in pairs.");
            }
            try {
                URIBuilder builder = new URIBuilder(uri);

                for (int index = 0; index < queryParameters.length; index = index + 2) {
                    builder.setParameter(queryParameters[index], queryParameters[index + 1]);
                }

                uriBuilt = builder.build();
                httpget = new HttpGet(uriBuilt);

            } catch (URISyntaxException e) {
                LOG.error("Error building request URI", e);
                return new ServiceClientResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);

            }

        }

        setHeaders(httpget, headers);
        return execute(httpget);
    }

    public int getPoolSize(){
        return httpClientService.getMaxConnections(connectionPoolId);
    }

    public int getSocketTimeout(){
        return httpClientService.getSocketTimeout(connectionPoolId);
    }

}
