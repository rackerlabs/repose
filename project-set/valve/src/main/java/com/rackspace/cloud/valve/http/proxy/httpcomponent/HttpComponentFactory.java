package com.rackspace.cloud.valve.http.proxy.httpcomponent;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public enum HttpComponentFactory {
  GET     ("GET",     HttpGet.class),
  PUT     ("PUT",     HttpPut.class,     EntityEnclosingMethodWrapper.class),
  POST    ("POST",    HttpPost.class,    EntityEnclosingMethodWrapper.class),
  DELETE  ("DELETE",  HttpDelete.class),
  HEAD    ("HEAD",    HttpHead.class),
  OPTIONS ("OPTIONS", HttpOptions.class);
  
  private static final Logger LOG = LoggerFactory.getLogger(HttpComponentFactory.class);
  private final String method;
  private final Class<? extends HttpRequestBase> httpClass;
  private final Class<? extends HttpComponentProcessableRequest> wrapperClass;
  
  HttpComponentFactory(String method, Class<? extends HttpRequestBase> httpClass) {
    this.method = method;
    this.httpClass = httpClass;
    this.wrapperClass = HttpMethodBaseWrapper.class;
  }
  
  HttpComponentFactory(String method, Class<? extends HttpRequestBase> httpClass, Class<? extends HttpComponentProcessableRequest> wrapperClass) {
    this.method = method;
    this.httpClass = httpClass;
    this.wrapperClass = wrapperClass;
  }
  
  private static HttpComponentProcessableRequest getInstance(HttpComponentFactory methodFactory, String uri) {
    HttpComponentProcessableRequest request = null;

    if (methodFactory != null) {
      try {
        Constructor<? extends HttpRequestBase> httpConstructor = methodFactory.httpClass.getConstructor(String.class);
        HttpRequestBase httpMethod = httpConstructor.newInstance(uri);
        
        Constructor<? extends HttpComponentProcessableRequest> constructor = (Constructor<? extends HttpComponentProcessableRequest>)methodFactory.wrapperClass.getConstructors()[0];
        request = constructor.newInstance(httpMethod);
      } catch (InvocationTargetException ex) {
        LOG.error("Unable to construct HttpMethod", ex);
      } catch (NoSuchMethodException ex) {
        LOG.error("Unable to construct HttpMethod", ex);
      } catch (InstantiationException ex) {
        LOG.error("Unable to construct HttpMethod", ex);
      } catch (IllegalAccessException ex) {
        LOG.error("Unable to construct HttpMethod", ex);
      }
    }
    
    return request;
  }
  
  public static HttpComponentProcessableRequest getMethod(String method, String uri) {
    HttpComponentFactory methodFactory = null;
    
    for (HttpComponentFactory item: HttpComponentFactory.values()) {
      if (item.method.equalsIgnoreCase(method)) {
        methodFactory = item;
        break;
      }
    }
    
    return getInstance(methodFactory, uri);
  }
}
/**
 * Wrap an entity enclosing http method which may send a request body.
 */
class EntityEnclosingMethodWrapper implements HttpComponentProcessableRequest {
    private final HttpEntityEnclosingRequestBase method;

    public EntityEnclosingMethodWrapper(HttpEntityEnclosingRequestBase method) {
      this.method = method;
    }

    @Override
    public HttpRequestBase process(HttpComponentRequestProcessor processor) throws IOException {
      return processor.process(method);
    }
}

/**
 * Wrap an http base type request that will not contain a request body
 * 
 */
class HttpMethodBaseWrapper implements HttpComponentProcessableRequest {
    private final HttpRequestBase method;

    public HttpMethodBaseWrapper(HttpRequestBase method) {
      this.method = method;
    }

    @Override
    public HttpRequestBase process(HttpComponentRequestProcessor processor) throws IOException {
      return processor.process(method);
    }

}
