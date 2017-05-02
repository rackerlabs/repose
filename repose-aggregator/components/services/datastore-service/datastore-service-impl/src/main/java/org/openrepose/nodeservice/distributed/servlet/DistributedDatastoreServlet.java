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
package org.openrepose.nodeservice.distributed.servlet;

import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.io.ObjectSerializer;
import org.openrepose.commons.utils.logging.TracingHeaderHelper;
import org.openrepose.commons.utils.logging.TracingKey;
import org.openrepose.core.services.datastore.*;
import org.openrepose.core.services.datastore.distributed.ClusterConfiguration;
import org.openrepose.core.services.datastore.distributed.ClusterView;
import org.openrepose.core.services.datastore.distributed.config.DistributedDatastoreConfiguration;
import org.openrepose.core.services.datastore.impl.distributed.CacheRequest;
import org.openrepose.core.services.datastore.impl.distributed.MalformedCacheRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static javax.servlet.http.HttpServletResponse.*;
import static org.openrepose.core.services.datastore.impl.distributed.MalformedCacheRequestError.*;

/**
 * Holds most of the work for running a distributed datastore.
 * Exposes the ClusterView and the ACL for update.
 * <p>
 * There's no reason we need this class serializable, and we don't want to imply we support it.
 * So, I'm suppressing the non-serializable fields warning instead of marking them transient.
 */
@SuppressWarnings("squid:S1948")
public class DistributedDatastoreServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedDatastoreServlet.class);
    private static final String DISTRIBUTED_HASH_RING = "distributed/hash-ring";
    private final ObjectSerializer objectSerializer = new ObjectSerializer(this.getClass().getClassLoader());
    private final AtomicReference<DatastoreAccessControl> hostAcl;
    private final DatastoreService datastoreService;
    private final ClusterConfiguration clusterConfiguration;
    private final DistributedDatastoreConfiguration ddConfig;
    private final Datastore localDatastore;

    public DistributedDatastoreServlet(
            DatastoreService datastore,
            ClusterConfiguration clusterConfiguration,
            DatastoreAccessControl acl,
            DistributedDatastoreConfiguration ddConfig
    ) {
        this.datastoreService = datastore;
        this.clusterConfiguration = clusterConfiguration;
        this.ddConfig = ddConfig;
        this.hostAcl = new AtomicReference<>(acl);
        localDatastore = datastore.getDefaultDatastore();
    }

    /**
     * Called from other threads to be able to tickle the cluster view for this servlet
     */
    public ClusterView getClusterView() {
        return clusterConfiguration.getClusterView();
    }

    /**
     * hit from other threads to update the ACL for this servlet.
     *
     * @param acl
     */
    public void updateAcl(DatastoreAccessControl acl) {
        this.hostAcl.set(acl);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        LOG.info("Registering datastore: {}", DISTRIBUTED_HASH_RING);

        boolean useHttps = ddConfig.getKeystoreFilename() != null;
        datastoreService.createDistributedDatastore(DISTRIBUTED_HASH_RING, clusterConfiguration, ddConfig.getConnectionPoolId(), useHttps);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (isRequestValid(request, response)) {
            String traceGUID = TracingHeaderHelper.getTraceGuid(request.getHeader(CommonHttpHeader.TRACE_GUID));
            MDC.put(TracingKey.TRACING_KEY, traceGUID);
            LOG.trace("SERVICING DISTDATASTORE REQUEST");

            if ("PATCH".equals(request.getMethod())) {
                doPatch(request, response);
            } else {
                super.service(request, response);
            }
            MDC.clear();
        }
    }

    @SuppressWarnings("squid:S1989")
    @Override
    protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // This servlet is only allowed to communicate with configured Repose instances,
        // so there is no chance of exposing sensitive information.
        // So it is safe to suppress warning squid:S1989
        resp.sendError(SC_METHOD_NOT_ALLOWED);
    }

    @SuppressWarnings("squid:S1989")
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // This servlet is only allowed to communicate with configured Repose instances,
        // so there is no chance of exposing sensitive information.
        // So it is safe to suppress warning squid:S1989
        try {
            CacheRequest cacheGet = CacheRequest.marshallCacheRequest(req);
            final Serializable value = localDatastore.get(cacheGet.getCacheKey());

            if (value != null) {
                resp.getOutputStream().write(objectSerializer.writeObject(value));
                resp.setStatus(SC_OK);

            } else {
                resp.setStatus(SC_NOT_FOUND);
            }
        } catch (MalformedCacheRequestException e) {

            LOG.error("Malformed cache request during GET", e);
            switch (e.getMessage()) {
                case CACHE_KEY_INVALID:
                    resp.getWriter().write(e.getMessage());
                    resp.setStatus(SC_NOT_FOUND);
                    break;
                case OBJECT_TOO_LARGE:
                case TTL_HEADER_NOT_POSITIVE:
                case UNEXPECTED_REMOTE_BEHAVIOR:
                    resp.setStatus(SC_BAD_REQUEST);
                    break;
                default:
                    resp.sendError(SC_INTERNAL_SERVER_ERROR);
                    break;
            }
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage(), ioe);
            resp.sendError(SC_INTERNAL_SERVER_ERROR);
        }
    }

    @SuppressWarnings("squid:S1989")
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // This servlet is only allowed to communicate with configured Repose instances,
        // so there is no chance of exposing sensitive information.
        // So it is safe to suppress warning squid:S1989
        if (CacheRequest.isCacheRequestValid(req)) {
            try {
                final CacheRequest cachePut = CacheRequest.marshallCacheRequestWithPayload(req);
                localDatastore.put(cachePut.getCacheKey(), objectSerializer.readObject(cachePut.getPayload()), cachePut.getTtlInSeconds(), TimeUnit.SECONDS);
                resp.setStatus(SC_ACCEPTED);
            } catch (IOException ioe) {
                LOG.error(ioe.getMessage(), ioe);
                throw new DatastoreOperationException("Failed to write payload.", ioe);
            } catch (ClassNotFoundException cnfe) {
                LOG.error(cnfe.getMessage(), cnfe);
                throw new DatastoreOperationException("Failed to deserialize a message. Couldn't find a matching class.", cnfe);
            } catch (MalformedCacheRequestException mcre) {
                handleputMalformedCacheRequestException(mcre, resp);
            }
        } else {
            resp.setStatus(SC_NOT_FOUND);
        }
    }

    @SuppressWarnings("squid:S1989")
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // This servlet is only allowed to communicate with configured Repose instances,
        // so there is no chance of exposing sensitive information.
        // So it is safe to suppress warning squid:S1989
        if (CacheRequest.isCacheRequestValid(req)) {
            try {
                final CacheRequest cacheDelete = CacheRequest.marshallCacheRequest(req);
                localDatastore.remove(cacheDelete.getCacheKey());
                resp.setStatus(SC_NO_CONTENT);
            } catch (MalformedCacheRequestException e) {
                LOG.trace("Malformed cache request on Delete", e);
                switch (e.getMessage()) {
                    case UNEXPECTED_REMOTE_BEHAVIOR:
                        resp.setStatus(SC_BAD_REQUEST);
                        break;
                    default:
                        resp.setStatus(SC_NO_CONTENT);
                        break;
                }
            }
        } else {
            resp.setStatus(SC_NOT_FOUND);
        }
    }

    @SuppressWarnings("squid:S1989")
    private void doPatch(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // This servlet is only allowed to communicate with configured Repose instances,
        // so there is no chance of exposing sensitive information.
        // So it is safe to suppress warning squid:S1989
        if (CacheRequest.isCacheRequestValid(request)) {
            try {
                final CacheRequest cachePatch = CacheRequest.marshallCacheRequestWithPayload(request);
                Serializable value = localDatastore.patch(cachePatch.getCacheKey(), (Patch) objectSerializer.readObject(cachePatch.getPayload()), cachePatch.getTtlInSeconds(), TimeUnit.SECONDS);
                response.getOutputStream().write(objectSerializer.writeObject(value));
                response.setStatus(SC_OK);
            } catch (IOException ioe) {
                LOG.error(ioe.getMessage(), ioe);
                throw new DatastoreOperationException("Failed to write payload.", ioe);
            } catch (ClassNotFoundException cnfe) {
                LOG.error(cnfe.getMessage(), cnfe);
                throw new DatastoreOperationException("Failed to deserialize a message. Couldn't find a matching class.", cnfe);
            } catch (MalformedCacheRequestException mcre) {
                LOG.trace("Handling Malformed Cache Request", mcre);
                handleputMalformedCacheRequestException(mcre, response);
            } catch (ClassCastException e) {
                LOG.trace("Sending ERROR response", e);
                response.sendError(SC_BAD_REQUEST, e.getMessage());
            }
        } else {
            response.setStatus(SC_NOT_FOUND);
        }
    }

    private boolean isAllowed(HttpServletRequest request) {
        boolean allowed = hostAcl.get().shouldAllowAll();

        if (!allowed) {
            try {
                final InetAddress remoteClient = InetAddress.getByName(request.getRemoteHost());

                for (InetAddress allowedAddress : hostAcl.get().getAllowedHosts()) {
                    if (remoteClient.equals(allowedAddress)) {
                        allowed = true;
                        break;
                    }
                }
            } catch (UnknownHostException uhe) {
                LOG.error("Unknown host exception caught while trying to resolve host: " + request.getRemoteHost() + " Reason: " + uhe.getMessage(), uhe);
            }
        }

        return allowed;
    }

    private boolean isRequestValid(HttpServletRequest req, HttpServletResponse resp) {
        boolean valid = false;
        if (!isAllowed(req)) {
            resp.setStatus(SC_UNAUTHORIZED);
        } else if (!CacheRequest.isCacheRequestValid(req)) {
            resp.setStatus(SC_NOT_FOUND);
        } else {
            valid = true;
        }

        return valid;
    }

    @Override
    public void destroy() {
        super.destroy();
        LOG.info("Unregistering Datastore: {}", DISTRIBUTED_HASH_RING);
        datastoreService.destroyDatastore(DISTRIBUTED_HASH_RING);
    }

    private void handleputMalformedCacheRequestException(MalformedCacheRequestException mcre, HttpServletResponse response) throws IOException {

        LOG.error("Handling Malformed Cache Request", mcre);
        switch (mcre.getMessage()) {
            case OBJECT_TOO_LARGE:
                response.getWriter().write(mcre.getMessage());
                response.setStatus(SC_REQUEST_ENTITY_TOO_LARGE);
                break;
            case CACHE_KEY_INVALID:
            case TTL_HEADER_NOT_POSITIVE:
            case UNEXPECTED_REMOTE_BEHAVIOR:
                response.getWriter().write(mcre.getMessage());
                response.setStatus(SC_BAD_REQUEST);
                break;
            default:
                response.sendError(SC_INTERNAL_SERVER_ERROR);
                break;
        }

    }
}
