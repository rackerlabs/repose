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
package org.openrepose.filters.clientauth.common

import org.openrepose.core.services.datastore.Datastore
import spock.lang.Specification

class EndpointsCacheTest extends Specification {

    def "when getting endpoints, the returned element should match what was passed in"() { //and be a string
        given:
        String tokenId = "tokenId"
        String endpointsData = "endpointsData"
        Integer ttl = 600000
        Datastore datastore = Mock()
        String cachePrefix = "cachePrefix"
        String cacheLoc = cachePrefix + "." + tokenId;
        EndpointsCache endpointsCache = new EndpointsCache(datastore, cachePrefix)
        endpointsCache.storeEndpoints(tokenId, endpointsData, ttl)
        datastore.get(cacheLoc) >> endpointsData

        when:
        String v = endpointsCache.getEndpoints(tokenId)

        then:
        v == endpointsData
    }
}
