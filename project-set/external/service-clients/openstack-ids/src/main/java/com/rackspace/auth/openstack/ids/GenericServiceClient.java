package com.rackspace.auth.openstack.ids;

import javax.net.ssl.*;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.ws.rs.ext.RuntimeDelegateImpl;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.logging.jersey.LoggingFilter;

import javax.ws.rs.ext.RuntimeDelegate;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * @author fran
 */
public class GenericServiceClient {
   private static final Logger LOG = LoggerFactory.getLogger(GenericServiceClient.class);
   
    static {
        // TODO: This should be removed, relocated or ignored. This is related to the issues we were seeing
        // where Jersey would work on some JVM installations but not all. This was rectified by adding a dependency
        // on jersey-server
        
        RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
    }

    private final Client client;
    private final String username, password;

    public GenericServiceClient(String username, String password) {
        this.username = username;
        this.password = password;
        
        DefaultClientConfig cc = new DefaultClientConfig();
        cc.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, false);
        // TODO: Eventually make these values configurable in Repose and implement
        // a "backoff" approach with logging.
        cc.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, 30000);
        cc.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, 30000);
        cc.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, createJerseySslHttpsProperties());        
        client = Client.create(cc);

        // TODO: Validate that this is required for all calls or only some
        HTTPBasicAuthFilter authFilter = new HTTPBasicAuthFilter(username, password);
        client.addFilter(authFilter);

        LOG.info("Enabling info logging of OpenStack Identity Service client requests");
        client.addFilter(new LoggingFilter());
    }

    private HTTPSProperties createJerseySslHttpsProperties() {
      // Create a trust manager that does not validate certificate chains
      TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){
          public X509Certificate[] getAcceptedIssuers(){return null;}
          public void checkClientTrusted(X509Certificate[] certs, String authType){}
          public void checkServerTrusted(X509Certificate[] certs, String authType){}
      }};

      // Install the all-trusting trust manager
      SSLContext sc = null;
      try {
          sc = SSLContext.getInstance("SSL");
          sc.init(null, trustAllCerts, new SecureRandom());
          HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      } catch (Exception e) {
          LOG.error("Problem creating SSL context: ", e);
      }

      return new HTTPSProperties( new ReposeHostnameVerifier(), sc);
   }

   private class ReposeHostnameVerifier implements HostnameVerifier {
      @Override
      public boolean verify(String hostname, SSLSession sslSession) {
         LOG.info("verifying: " + hostname);
         return true;
      }
   }

    public ServiceClientResponse getAdminToken(String uri) throws AuthServiceException {
        WebResource resource = client.resource(uri);

        PasswordCredentialsRequiredUsername credentials = new PasswordCredentialsRequiredUsername();
        credentials.setUsername(username);
        credentials.setPassword(password);

        // TODO: These QNames should come from the schema objects themselves
        JAXBElement jaxbCredentials = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0","passwordCredentials"), PasswordCredentialsRequiredUsername.class, credentials);
        AuthenticationRequest request = new AuthenticationRequest();
        request.setCredential(jaxbCredentials);

        // TODO: These QNames should come from the schema objects themselves
        JAXBElement jaxbRequest = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "auth"), AuthenticationRequest.class, request);
        ClientResponse response = resource.type(MediaType.APPLICATION_XML_TYPE).header("Accept", "application/xml").post(ClientResponse.class, jaxbRequest);

        return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
    }

    public ServiceClientResponse get(String uri, String adminToken, String... queryParameters) throws AuthServiceException {
        WebResource resource = client.resource(uri);

        if (queryParameters.length % 2 != 0) {
           throw new IllegalArgumentException("Query parameters must be in pairs.");
        }

        for (int index = 0; index < queryParameters.length; index = index + 2) {
           resource = resource.queryParam(queryParameters[index], queryParameters[index + 1]);
        }

        ClientResponse response = resource.header("Accept", "application/xml").header("X-Auth-Token", adminToken).get(ClientResponse.class);
        return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
    }
}
