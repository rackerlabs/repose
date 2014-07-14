package com.rackspace.papi.service.context.container

import com.rackspace.papi.domain.ServicePorts
import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.nullValue
import static org.junit.Assert.assertThat
/**
 * Created by eric7500 on 7/10/14.
 */
public class ContainerConfigurationServiceImplTest {


    @Test
    public void testConstructorWithDummyValues() {
        ContainerConfigurationService testImpl = new ContainerConfigurationServiceImpl(new ServicePorts(),null);
        assertThat(testImpl.ports,equalTo(new ServicePorts()));
        assertThat(testImpl.configurationManager,nullValue());
    }

    @Test
    public void testVia() {
        ContainerConfigurationService testImpl = new ContainerConfigurationServiceImpl(new ServicePorts(),null);
        testImpl.setVia("testVia");
        assertThat(testImpl.getVia(),equalTo("testVia"));
    }


    @Test
    public void testContentBodyReadLimitNull() {
        ContainerConfigurationService testImpl = new ContainerConfigurationServiceImpl(new ServicePorts(),null);
        testImpl.setContentBodyReadLimit(null);
        assertThat(testImpl.getContentBodyReadLimit(),equalTo(0L))
    }

    @Test
    public void testContentBodyReadLimitNotNull() {
        ContainerConfigurationService testImpl = new ContainerConfigurationServiceImpl(new ServicePorts(),null);
        testImpl.setContentBodyReadLimit(5L);
        assertThat(testImpl.getContentBodyReadLimit(),equalTo(5L))
    }

    @Test
    public void testGetServicePorts() {
        ContainerConfigurationService testImpl = new ContainerConfigurationServiceImpl(new ServicePorts(),null);
        assertThat(testImpl.getServicePorts(),equalTo(new ServicePorts()));
    }
}
