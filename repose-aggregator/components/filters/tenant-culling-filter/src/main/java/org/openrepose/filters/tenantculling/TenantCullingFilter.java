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
package org.openrepose.filters.tenantculling;

import org.openrepose.commons.utils.io.ObjectSerializer;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.datastore.DatastoreService;
import org.openrepose.filters.keystonev2.KeystoneRequestHandler;
import org.openrepose.filters.keystonev2.KeystoneV2Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.JavaConverters;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.TENANT_ID;

/**
 * Created by adrian on 6/12/17.
 */
@Named
public class TenantCullingFilter implements Filter {

    public static final String RELEVANT_ROLES = "X-Relevant-Roles";

    private Logger log = LoggerFactory.getLogger(TenantCullingFilter.class);
    private Datastore datastore;
    private ObjectSerializer objectSerializer;

    @Inject
    public TenantCullingFilter(DatastoreService datastoreService) {
        datastore = datastoreService.getDefaultDatastore();
        objectSerializer = new ObjectSerializer(this.getClass().getClassLoader());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //do nothing
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequestWrapper request = new HttpServletRequestWrapper((HttpServletRequest) servletRequest);
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String cacheKey = request.getHeader(KeystoneV2Filter.AuthTokenKey());
        List<String> relevantRoles = request.getSplittableHeaders(RELEVANT_ROLES);
        if (cacheKey != null) {
            try {
                                                                                              //this is a dirty hack, i have no idea why it has a ClassCastException without it
                KeystoneRequestHandler.ValidToken token = (KeystoneRequestHandler.ValidToken) objectSerializer.readObject(objectSerializer.writeObject(datastore.get(cacheKey)));
                if (token != null) {
                    Set<String> tenants = JavaConverters.seqAsJavaListConverter(token.roles()).asJava()
                            .stream()
                            .filter(role -> relevantRoles.contains(role.name()))
                            .filter(role -> role.tenantId().isDefined())
                            .map(role -> role.tenantId().get())
                            .collect(Collectors.toSet());

                    if (token.defaultTenantId().isDefined()) {
                        tenants.add(token.defaultTenantId().get());
                    }

                    request.removeHeader(TENANT_ID);

                    tenants.forEach(tenant -> request.addHeader(TENANT_ID, tenant));

                    chain.doFilter(request, response);
                } else {
                    log.debug("Cache miss for key: {}", cacheKey);
                    response.sendError(SC_UNAUTHORIZED);
                }
            } catch (ClassNotFoundException cnfe) {
                log.error("This shouldn't have been possible, somehow the item that came back from datastore doesn't match a class available in the current classloader", cnfe);
                response.sendError(SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            log.debug("Cache key header not found");
            response.sendError(SC_UNAUTHORIZED);
        }
    }

    @Override
    public void destroy() {
        //do nothing
    }
}
