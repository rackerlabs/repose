package com.rackspace.auth.openstack

import spock.lang.Specification

class AuthenticationServiceClientGroovyTest extends Specification {

    def "when converting a stream, it should return a base 64 encoded string"() {
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
