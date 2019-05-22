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
package org.openrepose.core.services.datastore;

import org.openrepose.commons.utils.encoding.EncodingProvider;
import org.openrepose.core.services.RequestProxyService;
import org.openrepose.core.services.datastore.distributed.ClusterConfiguration;
import org.openrepose.core.services.datastore.distributed.DistributedDatastore;

import java.net.InetSocketAddress;

/**
 * DatastoreService - service that manages the lifecycle and configuration of {@link Datastore}s
 */
public interface DatastoreService {

    /**
     * Get the default datastore
     */
    Datastore getDefaultDatastore() throws DatastoreUnavailableException;

    /**
     * Get a datastore associated with the provided datastore name
     *
     * @param datastoreName name for the Datastore
     * @return the Datastore with the provided name
     * @throws DatastoreUnavailableException if no datastore exists with the given datastoreName
     */
    Datastore getDatastore(String datastoreName) throws DatastoreUnavailableException;

    /**
     * Get the distributed datastore managed by the service.
     *
     * @return the default Distributed Datastore
     * @throws DatastoreUnavailableException if no distributed datastore exists
     */
    DistributedDatastore getDistributedDatastore() throws DatastoreUnavailableException;

    /**
     * Shutdown the datastore associated with the datastore name
     *
     * @param datastoreName unique name for the Datastore
     */
    void destroyDatastore(String datastoreName);

    /**
     * Create and return a distributed datastore using the provided configuration.  The created
     * datastore can be retrieved by the same name provided using getDatastore(datastoreName)
     *
     * @param datastoreName unique name for this Datastore
     * @param configuration Configuration for the entire Cluster
     * @return the newly created Datastore
     * @throws DatastoreServiceException if the datastore creation fails
     */
    DistributedDatastore createDatastore(String datastoreName, ClusterConfiguration configuration)
            throws DatastoreServiceException;

    /**
     * Create and return a distributed datastore using the provided configuration.  The created
     * datastore can be retrieved by the same name provided using getDatastore(datastoreName)
     *
     * @param datastoreName unique name for this Datastore
     * @param configuration Configuration for the entire Cluster
     * @param connPoolId    the name of the pool to borrow a connection from
     * @param useHttps      indicates if SSL/TLS should be used
     * @return the newly created Datastore
     * @throws DatastoreServiceException if the datastore creation fails
     */
    DistributedDatastore createDistributedDatastore(String datastoreName, ClusterConfiguration configuration, String connPoolId, boolean useHttps)
            throws DatastoreServiceException;

    /**
     * Shutdown all datastores
     */
    void shutdown();
}
