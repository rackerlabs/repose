package com.rackspace.papi

import com.rackspace.papi.servlet.InitParameter
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.UnavailableException

class EmptyServletTest extends Specification {
    EmptyServlet emptyServlet = new EmptyServlet()

    @Unroll('servlet init where Cluster ID Present: ${clusterIdPresent}, Node ID Present: ${nodeIdPresent} should fail')
    def "when required properties are not provided, the servlet should be marked unavailable"() throws Throwable {
        given:
        if (clusterIdPresent) {
            System.setProperty(InitParameter.REPOSE_CLUSTER_ID.parameterName, "repose")
        } else {
            System.clearProperty(InitParameter.REPOSE_CLUSTER_ID.parameterName)
        }

        if (nodeIdPresent) {
            System.setProperty(InitParameter.REPOSE_NODE_ID.parameterName, "node")
        } else {
            System.clearProperty(InitParameter.REPOSE_NODE_ID.parameterName)
        }

        when:
        emptyServlet.init()

        then:
        thrown(UnavailableException)

        where:
        clusterIdPresent | nodeIdPresent
        false            | false
        false            | true
        true             | false
    }

    def "when repose-cluster-id and repose-node-id are provided, initialization should pass"() throws Throwable {
        given:
        System.setProperty(InitParameter.REPOSE_CLUSTER_ID.parameterName, "repose")
        System.setProperty(InitParameter.REPOSE_NODE_ID.parameterName, "node")

        when:
        emptyServlet.init()

        then:
        notThrown(UnavailableException)
    }
}
