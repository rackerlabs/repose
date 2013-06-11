package com.rackspace.papi.components.clientauth.common

import com.rackspace.papi.commons.util.io.ObjectSerializer
import com.rackspace.papi.service.datastore.Datastore
import com.rackspace.papi.service.datastore.StoredElement
import com.rackspace.papi.service.datastore.impl.StoredElementImpl
import spock.lang.Specification

import static org.hamcrest.CoreMatchers.any

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
        StoredElement element = new StoredElementImpl(cacheLoc, ObjectSerializer.instance().writeObject(endpointsData));
        datastore.get(cacheLoc) >> element
        endpointsCache.getElementAsType(any(StoredElement.class)) >> endpointsData

        when:
        String v = endpointsCache.getEndpoints(tokenId)

        then:
        v == endpointsData
        //v.is(instanceOf(String.class))
    }
}
