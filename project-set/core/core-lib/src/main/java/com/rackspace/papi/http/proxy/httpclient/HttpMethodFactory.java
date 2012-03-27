package com.rackspace.papi.http.proxy.httpclient;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum HttpMethodFactory {
  GET     ("GET",     GetMethod.class),
  PUT     ("PUT",     PutMethod.class,     EntityEnclosingMethodWrapper.class),
  POST    ("POST",    PostMethod.class,    EntityEnclosingMethodWrapper.class),
  DELETE  ("DELETE",  DeleteMethod.class),
  HEAD    ("HEAD",    HeadMethod.class),
  OPTIONS ("OPTIONS", OptionsMethod.class);
  
  private static final Logger LOG = LoggerFactory.getLogger(HttpMethodFactory.class);
  private static final String CONSTRUCTION_ERROR = "Unable to construct HttpMethod";
  private final String method;
  private final Class<? extends HttpMethodBase> httpClass;
  private final Class<? extends ProcessableRequest> wrapperClass;
  
  HttpMethodFactory(String method, Class<? extends HttpMethodBase> httpClass) {
    this.method = method;
    this.httpClass = httpClass;
    this.wrapperClass = HttpMethodBaseWrapper.class;
  }
  
  HttpMethodFactory(String method, Class<? extends HttpMethodBase> httpClass, Class<? extends ProcessableRequest> wrapperClass) {
    this.method = method;
    this.httpClass = httpClass;
    this.wrapperClass = wrapperClass;
  }
  
  private static ProcessableRequest getInstance(HttpMethodFactory methodFactory, String uri) {
    ProcessableRequest request = null;

    if (methodFactory != null) {
      try {
        Constructor<? extends HttpMethodBase> httpConstructor = methodFactory.httpClass.getConstructor(String.class);
        HttpMethod httpMethod = httpConstructor.newInstance(uri);
        
        Constructor<? extends ProcessableRequest> constructor = (Constructor<? extends ProcessableRequest>)methodFactory.wrapperClass.getConstructors()[0];
        request = constructor.newInstance(httpMethod);
      } catch (InvocationTargetException ex) {
        LOG.error(CONSTRUCTION_ERROR, ex);
      } catch (NoSuchMethodException ex) {
        LOG.error(CONSTRUCTION_ERROR, ex);
      } catch (InstantiationException ex) {
        LOG.error(CONSTRUCTION_ERROR, ex);
      } catch (IllegalAccessException ex) {
        LOG.error(CONSTRUCTION_ERROR, ex);
      }
    }
    
    return request;
  }
  
  public static ProcessableRequest getMethod(String method, String uri) {
    HttpMethodFactory methodFactory = null;
    
    for (HttpMethodFactory item: HttpMethodFactory.values()) {
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
class EntityEnclosingMethodWrapper implements ProcessableRequest {
    private final EntityEnclosingMethod method;

    public EntityEnclosingMethodWrapper(EntityEnclosingMethod method) {
      this.method = method;
    }

    @Override
    public HttpMethod process(HttpRequestProcessor processor) throws IOException {
      return processor.process(method);
    }
}

/**
 * Wrap an http base type request that will not contain a request body
 * 
 */
class HttpMethodBaseWrapper implements ProcessableRequest {
    private final HttpMethodBase method;

    public HttpMethodBaseWrapper(HttpMethodBase method) {
      this.method = method;
    }

    @Override
    public HttpMethod process(HttpRequestProcessor processor) throws IOException {
      return processor.process(method);
    }

}
