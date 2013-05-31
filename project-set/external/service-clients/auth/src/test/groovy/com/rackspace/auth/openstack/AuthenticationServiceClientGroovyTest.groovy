package com.rackspace.auth.openstack

import spock.lang.Specification

class AuthenticationServiceClientGroovyTest extends Specification {

    def "testing the private convertStreamToBase64String method"() {
        given:
        def AuthenticationServiceClient asc = new AuthenticationServiceClient(null, null, null, null, null, null, null)
        def InputStream inputStream = new ByteArrayInputStream("test".getBytes())
        def String s

        when:
        s = asc.convertStreamToBase64(inputStream)

        then:
        s == "dGVzdA=="
    }
}
