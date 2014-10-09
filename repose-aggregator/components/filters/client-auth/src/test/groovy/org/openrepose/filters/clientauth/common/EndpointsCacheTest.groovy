package org.openrepose.filters.clientauth.common
import com.rackspace.papi.components.datastore.Datastore
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
