package com.rackspace.auth.openstack

import com.rackspace.auth.AuthServiceException
import com.rackspace.papi.commons.util.transform.jaxb.JaxbEntityToXml
import org.junit.Test

import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException

class AuthenticationServiceClientGroovyTest {


    @Test
    void whenConvertingAStreamItShouldReturnABase64EncodedString() {
        JAXBContext coreJaxbContext;
        JAXBContext groupJaxbContext;

        try {
            coreJaxbContext = JAXBContext.newInstance(
                    org.openstack.docs.identity.api.v2.ObjectFactory.class,
                    com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory.class);
            groupJaxbContext = JAXBContext.newInstance(com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory.class);
        } catch (JAXBException e) {
            LOG.error("Problem creating the jaxb context for the OpenStack Auth objects.", e);
            throw new AuthServiceException("Possible deployment problem! Unable to build JAXB Context for OpenStack Auth schema types. Reason: "
                    + e.getMessage(), e);
        }



        def AuthenticationServiceClient asc = new AuthenticationServiceClient("http:hostname.com", "user", "pass", "id", null, null, new JaxbEntityToXml(coreJaxbContext), null)
        def InputStream inputStream = new ByteArrayInputStream("test".getBytes())
        def String s

        s = asc.convertStreamToBase64String(inputStream)

        assert s == "dGVzdA=="
    }
}
