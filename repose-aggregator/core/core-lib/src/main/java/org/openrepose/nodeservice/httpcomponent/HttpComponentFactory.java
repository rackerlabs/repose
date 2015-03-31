/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.nodeservice.httpcomponent;

import org.apache.http.client.methods.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

public enum HttpComponentFactory {

    GET("GET", HttpGet.class),
    PUT("PUT", HttpPut.class, EntityEnclosingMethodWrapper.class),
    POST("POST", HttpPost.class, EntityEnclosingMethodWrapper.class),
    DELETE("DELETE", EntityEnclosingDelete.class, EntityEnclosingMethodWrapper.class),
    HEAD("HEAD", HttpHead.class),
    OPTIONS("OPTIONS", HttpOptions.class),
    PATCH("PATCH", HttpPatch.class, EntityEnclosingMethodWrapper.class),
    TRACE("TRACE", HttpTrace.class);
    private static final Logger LOG = LoggerFactory.getLogger(HttpComponentFactory.class);
    private static final String CONSTRUCTION_ERROR = "Unable to construct HttpMethod";
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

                Constructor<? extends HttpComponentProcessableRequest> constructor = (Constructor<? extends HttpComponentProcessableRequest>) methodFactory.wrapperClass.getConstructors()[0];
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

    private static HttpComponentProcessableRequest getInstance(HttpComponentFactory methodFactory, URI uri) {
        HttpComponentProcessableRequest request = null;

        if (methodFactory != null) {
            try {
                Constructor<? extends HttpRequestBase> httpConstructor = methodFactory.httpClass.getConstructor(URI.class);
                HttpRequestBase httpMethod = httpConstructor.newInstance(uri);

                Constructor<? extends HttpComponentProcessableRequest> constructor = (Constructor<? extends HttpComponentProcessableRequest>) methodFactory.wrapperClass.getConstructors()[0];
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

    public static HttpComponentProcessableRequest getMethod(String method, String uri) {
        HttpComponentFactory methodFactory = null;

        for (HttpComponentFactory item : HttpComponentFactory.values()) {
            if (item.method.equalsIgnoreCase(method)) {
                methodFactory = item;
                break;
            }
        }

        return getInstance(methodFactory, uri);
    }

    public static HttpComponentProcessableRequest getMethod(String method, URI uri) {
        HttpComponentFactory methodFactory = null;

        for (HttpComponentFactory item : HttpComponentFactory.values()) {
            if (item.method.equalsIgnoreCase(method)) {
                methodFactory = item;
                break;
            }
        }

        return getInstance(methodFactory, uri);
    }
}

class EntityEnclosingDelete extends HttpEntityEnclosingRequestBase {

    private static final Logger LOG = LoggerFactory.getLogger(EntityEnclosingDelete.class);

    public EntityEnclosingDelete(String uri) {
        try {
            super.setURI(new URI(uri));
        } catch (URISyntaxException ex) {
            LOG.error("Invalid URI: " + uri, ex);
        }
    }

    public EntityEnclosingDelete(URI uri) {
        super.setURI(uri);
    }

    @Override
    public String getMethod() {
        return "DELETE";
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
