package com.rackspace.papi.service.proxy.jersey;

import com.rackspace.papi.http.proxy.common.HttpResponseCodeProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;

/**
 * @author fran
 */
public class ReposeHttpsUrlConnection extends HttpsURLConnection {

   private static final Logger LOG = LoggerFactory.getLogger(ReposeHttpsUrlConnection.class);

   private final HttpsURLConnection httpsUrlConnection;

   public ReposeHttpsUrlConnection(URL url) throws IOException {
      super(url);
      httpsUrlConnection = (HttpsURLConnection) url.openConnection();
   }

   @Override
   public String getCipherSuite() {
      return httpsUrlConnection.getCipherSuite();
   }

   @Override
   public Certificate[] getLocalCertificates() {
      return httpsUrlConnection.getLocalCertificates();
   }

   @Override
   public Certificate[] getServerCertificates() throws SSLPeerUnverifiedException {
      return httpsUrlConnection.getServerCertificates();
   }

   @Override
   public void disconnect() {
      httpsUrlConnection.disconnect();
   }

   @Override
   public boolean usingProxy() {
      return httpsUrlConnection.usingProxy();
   }

   @Override
   public void connect() throws IOException {
      httpsUrlConnection.connect();
   }

   @Override
   public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
      return httpsUrlConnection.getPeerPrincipal();
   }

   @Override
   public Principal getLocalPrincipal() {
      return httpsUrlConnection.getLocalPrincipal();
   }

   @Override
   public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
      httpsUrlConnection.setHostnameVerifier(hostnameVerifier);
   }

   @Override
   public HostnameVerifier getHostnameVerifier() {
      return httpsUrlConnection.getHostnameVerifier();
   }

   @Override
   public void setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
      httpsUrlConnection.setSSLSocketFactory(sslSocketFactory);
   }

   @Override
   public SSLSocketFactory getSSLSocketFactory() {
      return httpsUrlConnection.getSSLSocketFactory();
   }

   @Override
   public String getHeaderFieldKey(int i) {
      return httpsUrlConnection.getHeaderFieldKey(i);
   }

   @Override
   public void setChunkedStreamingMode(int i) {
      httpsUrlConnection.setChunkedStreamingMode(i);
   }

   @Override
   public String getHeaderField(int i) {
      return httpsUrlConnection.getHeaderField(i);
   }

   @Override
   public void setInstanceFollowRedirects(boolean b) {
      httpsUrlConnection.setInstanceFollowRedirects(b);
   }

   @Override
   public boolean getInstanceFollowRedirects() {
      return httpsUrlConnection.getInstanceFollowRedirects();
   }

   @Override
   public void setRequestMethod(String s) throws ProtocolException {
      httpsUrlConnection.setRequestMethod(s);
   }

   @Override
   public String getRequestMethod() {
      return httpsUrlConnection.getRequestMethod();
   }

   @Override
   public int getResponseCode() throws IOException {
      return httpsUrlConnection.getResponseCode();
   }

   @Override
   public String getResponseMessage() throws IOException {
      return httpsUrlConnection.getResponseMessage();
   }

   @Override
   public long getHeaderFieldDate(String s, long l) {
      return httpsUrlConnection.getHeaderFieldDate(s, l);
   }

   @Override
   public Permission getPermission() throws IOException {
      return httpsUrlConnection.getPermission();
   }

   @Override
   public void setConnectTimeout(int i) {
      httpsUrlConnection.setConnectTimeout(i);
   }

   @Override
   public int getConnectTimeout() {
      return httpsUrlConnection.getConnectTimeout();
   }

   @Override
   public void setReadTimeout(int i) {
      httpsUrlConnection.setReadTimeout(i);
   }

   @Override
   public int getReadTimeout() {
      return httpsUrlConnection.getReadTimeout();
   }

   @Override
   public URL getURL() {
      return httpsUrlConnection.getURL();
   }

   @Override
   public int getContentLength() {
      return httpsUrlConnection.getContentLength();
   }

   @Override
   public String getContentType() {
      return httpsUrlConnection.getContentType();
   }

   @Override
   public String getContentEncoding() {
      return httpsUrlConnection.getContentEncoding();
   }

   @Override
   public long getExpiration() {
      return httpsUrlConnection.getExpiration();
   }

   @Override
   public long getDate() {
      return httpsUrlConnection.getDate();
   }

   @Override
   public long getLastModified() {
      return httpsUrlConnection.getLastModified();
   }

   @Override
   public String getHeaderField(String s) {
      return httpsUrlConnection.getHeaderField(s);
   }

   @Override
   public Map<String, List<String>> getHeaderFields() {
      return httpsUrlConnection.getHeaderFields();
   }

   @Override
   public int getHeaderFieldInt(String s, int i) {
      return httpsUrlConnection.getHeaderFieldInt(s, i);
   }

   @Override
   public Object getContent() throws IOException {
      return httpsUrlConnection.getContent();
   }

   @Override
   public Object getContent(Class[] classes) throws IOException {
      return httpsUrlConnection.getContent(classes);
   }

   @Override
   public InputStream getInputStream() throws IOException {
      return httpsUrlConnection.getInputStream();
   }

   @Override
   public OutputStream getOutputStream() throws IOException {
      return httpsUrlConnection.getOutputStream();
   }

   @Override
   public String toString() {
      return httpsUrlConnection.toString();
   }

   @Override
   public void setDoInput(boolean b) {
      httpsUrlConnection.setDoInput(b);
   }

   @Override
   public boolean getDoInput() {
      return httpsUrlConnection.getDoInput();
   }

   @Override
   public void setDoOutput(boolean b) {
      httpsUrlConnection.setDoOutput(b);
   }

   @Override
   public boolean getDoOutput() {
      return httpsUrlConnection.getDoOutput();
   }

   @Override
   public void setAllowUserInteraction(boolean b) {
      httpsUrlConnection.setAllowUserInteraction(b);
   }

   @Override
   public boolean getAllowUserInteraction() {
      return httpsUrlConnection.getAllowUserInteraction();
   }

   @Override
   public void setUseCaches(boolean b) {
      httpsUrlConnection.setUseCaches(b);
   }

   @Override
   public boolean getUseCaches() {
      return httpsUrlConnection.getUseCaches();
   }

   @Override
   public void setIfModifiedSince(long l) {
      httpsUrlConnection.setIfModifiedSince(l);
   }

   @Override
   public long getIfModifiedSince() {
      return httpsUrlConnection.getIfModifiedSince();
   }

   @Override
   public boolean getDefaultUseCaches() {
      return httpsUrlConnection.getDefaultUseCaches();
   }

   @Override
   public void setDefaultUseCaches(boolean b) {
      httpsUrlConnection.setDefaultUseCaches(b);
   }

   @Override
   public void setRequestProperty(String s, String s1) {
      httpsUrlConnection.setRequestProperty(s, s1);
   }

   @Override
   public void addRequestProperty(String s, String s1) {
      httpsUrlConnection.addRequestProperty(s, s1);
   }

   @Override
   public String getRequestProperty(String s) {
      return httpsUrlConnection.getRequestProperty(s);
   }

   @Override
   public Map<String, List<String>> getRequestProperties() {
      return httpsUrlConnection.getRequestProperties();
   }

   @Override
   public int hashCode() {
      return httpsUrlConnection.hashCode();
   }

   @Override
   public boolean equals(Object o) {
      return httpsUrlConnection.equals(o);
   }

   @Override
   public void setFixedLengthStreamingMode(int i) {
      //httpsUrlConnection.setFixedLengthStreamingMode(i);
   }

   @Override
   public InputStream getErrorStream() {
      InputStream stream = httpsUrlConnection.getErrorStream();

      if (stream == null) {
         try {
            HttpResponseCodeProcessor responseCode = new HttpResponseCodeProcessor(httpsUrlConnection.getResponseCode());

            if (responseCode.getCode() >= 300) {
               LOG.info("Getting input stream on redirect.");
               stream = httpsUrlConnection.getInputStream();
            }
         } catch (IOException e) {
            LOG.info("IOException when reading from Input Stream for redirect response code.", e);
         }
      }

      return stream;
   }
}
