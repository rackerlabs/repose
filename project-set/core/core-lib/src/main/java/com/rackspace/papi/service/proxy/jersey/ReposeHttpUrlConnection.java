package com.rackspace.papi.service.proxy.jersey;

import com.rackspace.papi.http.proxy.common.HttpResponseCodeProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.util.List;
import java.util.Map;

/**
 * @author fran
 */
public class ReposeHttpUrlConnection extends HttpURLConnection {

   private static final Logger LOG = LoggerFactory.getLogger(ReposeHttpUrlConnection.class);


   private final HttpURLConnection httpUrlConnection;

   public ReposeHttpUrlConnection(URL url) throws IOException {
      super(url);
      httpUrlConnection = (HttpURLConnection) url.openConnection();             
   }

   @Override
   public void disconnect() {
      httpUrlConnection.disconnect();
   }

   @Override
   public boolean usingProxy() {
      return httpUrlConnection.usingProxy();
   }

   @Override
   public void connect() throws IOException {
      httpUrlConnection.connect();
   }

   @Override
   public int getResponseCode() throws IOException {
      return httpUrlConnection.getResponseCode();
   }

   @Override
   public InputStream getInputStream() throws IOException {
      return httpUrlConnection.getInputStream();
   }

   @Override
   public String getHeaderFieldKey(int i) {
      return httpUrlConnection.getHeaderFieldKey(i);
   }

   @Override
   public void setChunkedStreamingMode(int i) {
      httpUrlConnection.setChunkedStreamingMode(i);
   }

   @Override
   public String getHeaderField(int i) {
      return httpUrlConnection.getHeaderField(i);
   }

   @Override
   public void setInstanceFollowRedirects(boolean b) {
      httpUrlConnection.setInstanceFollowRedirects(b);
   }

   @Override
   public boolean getInstanceFollowRedirects() {
      return httpUrlConnection.getInstanceFollowRedirects();
   }

   @Override
   public void setRequestMethod(String s) throws ProtocolException {
      httpUrlConnection.setRequestMethod(s);
   }

   @Override
   public String getRequestMethod() {
      return httpUrlConnection.getRequestMethod();
   }

   @Override
   public String getResponseMessage() throws IOException {
      return httpUrlConnection.getResponseMessage();
   }

   @Override
   public long getHeaderFieldDate(String s, long l) {
      return httpUrlConnection.getHeaderFieldDate(s, l);
   }

   @Override
   public Permission getPermission() throws IOException {
      return httpUrlConnection.getPermission();
   }

   @Override
   public void setConnectTimeout(int i) {
      httpUrlConnection.setConnectTimeout(i);
   }

   @Override
   public int getConnectTimeout() {
      return httpUrlConnection.getConnectTimeout();
   }

   @Override
   public void setReadTimeout(int i) {
      httpUrlConnection.setReadTimeout(i);
   }

   @Override
   public int getReadTimeout() {
      return httpUrlConnection.getReadTimeout();
   }

   @Override
   public URL getURL() {
      return httpUrlConnection.getURL();
   }

   @Override
   public int getContentLength() {
      return httpUrlConnection.getContentLength();
   }

   @Override
   public String getContentType() {
      return httpUrlConnection.getContentType();
   }

   @Override
   public String getContentEncoding() {
      return httpUrlConnection.getContentEncoding();
   }

   @Override
   public long getExpiration() {
      return httpUrlConnection.getExpiration();
   }

   @Override
   public long getDate() {
      return httpUrlConnection.getDate();
   }

   @Override
   public long getLastModified() {
      return httpUrlConnection.getLastModified();
   }

   @Override
   public String getHeaderField(String s) {
      return httpUrlConnection.getHeaderField(s);
   }

   @Override
   public Map<String, List<String>> getHeaderFields() {
      return httpUrlConnection.getHeaderFields();
   }

   @Override
   public int getHeaderFieldInt(String s, int i) {
      return httpUrlConnection.getHeaderFieldInt(s, i);
   }

   @Override
   public Object getContent() throws IOException {
      return httpUrlConnection.getContent();
   }

   @Override
   public Object getContent(Class[] classes) throws IOException {
      return httpUrlConnection.getContent(classes);
   }

   @Override
   public OutputStream getOutputStream() throws IOException {
      return httpUrlConnection.getOutputStream();
   }

   @Override
   public String toString() {
      return httpUrlConnection.toString();
   }

   @Override
   public void setDoInput(boolean b) {
      httpUrlConnection.setDoInput(b);
   }

   @Override
   public boolean getDoInput() {
      return httpUrlConnection.getDoInput();
   }

   @Override
   public void setDoOutput(boolean b) {
      httpUrlConnection.setDoOutput(b);
   }

   @Override
   public boolean getDoOutput() {
      return httpUrlConnection.getDoOutput();
   }

   @Override
   public void setAllowUserInteraction(boolean b) {
      httpUrlConnection.setAllowUserInteraction(b);
   }

   @Override
   public boolean getAllowUserInteraction() {
      return httpUrlConnection.getAllowUserInteraction();
   }

   @Override
   public void setUseCaches(boolean b) {
      httpUrlConnection.setUseCaches(b);
   }

   @Override
   public boolean getUseCaches() {
      return httpUrlConnection.getUseCaches();
   }

   @Override
   public void setIfModifiedSince(long l) {
      httpUrlConnection.setIfModifiedSince(l);
   }

   @Override
   public long getIfModifiedSince() {
      return httpUrlConnection.getIfModifiedSince();
   }

   @Override
   public boolean getDefaultUseCaches() {
      return httpUrlConnection.getDefaultUseCaches();
   }

   @Override
   public void setDefaultUseCaches(boolean b) {
      httpUrlConnection.setDefaultUseCaches(b);
   }

   @Override
   public void setRequestProperty(String s, String s1) {
      httpUrlConnection.setRequestProperty(s, s1);
   }

   @Override
   public void addRequestProperty(String s, String s1) {
      httpUrlConnection.addRequestProperty(s, s1);
   }

   @Override
   public String getRequestProperty(String s) {
      return httpUrlConnection.getRequestProperty(s);
   }

   @Override
   public Map<String, List<String>> getRequestProperties() {
      return httpUrlConnection.getRequestProperties();
   }

   @Override
   public int hashCode() {
      return httpUrlConnection.hashCode();
   }

   @Override
   public boolean equals(Object o) {
      return httpUrlConnection.equals(o);
   }

   @Override
   public void setFixedLengthStreamingMode(int i) {
      //httpUrlConnection.setFixedLengthStreamingMode(i);
   }

   @Override
   public InputStream getErrorStream() {
      InputStream stream = httpUrlConnection.getErrorStream();

      if (stream == null) {
         try {
            HttpResponseCodeProcessor responseCode = new HttpResponseCodeProcessor(httpUrlConnection.getResponseCode());

            if (responseCode.isRedirect()) {
               LOG.info("Getting input stream on redirect.");
               stream = httpUrlConnection.getInputStream();
            }
         } catch (IOException e) {
            LOG.info("IOException when reading from Input Stream for redirect response code.", e);
         }
      }

      return stream;
   }   
}
