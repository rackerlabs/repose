package com.rackspace.papi.service.context.container

import com.rackspace.papi.container.config.DeploymentConfiguration
import org.junit.Test
import org.openrepose.core.service.config.ConfigurationService

import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat
import static org.mockito.Mockito.mock

public class ContainerConfigurationServiceImplTest {

    @Test
    public void testVia() {
        ContainerConfigurationService testImpl = new ContainerConfigurationServiceImpl(mock(ConfigurationService));
        testImpl.setVia("testVia");
        assertThat(testImpl.getVia(),equalTo("testVia"));
    }


    @Test
    public void testContentBodyReadLimitNull() {
        ContainerConfigurationService testImpl = new ContainerConfigurationServiceImpl(mock(ConfigurationService));
        testImpl.setContentBodyReadLimit(null);
        assertThat(testImpl.getContentBodyReadLimit(),equalTo(0L))
    }

    @Test
    public void testContentBodyReadLimitNotNull() {
        ContainerConfigurationService testImpl = new ContainerConfigurationServiceImpl(mock(ConfigurationService));
        testImpl.setContentBodyReadLimit(5L);
        assertThat(testImpl.getContentBodyReadLimit(),equalTo(5L))
    }


    @Test
    public void testDeprecatedConfigs() {
        ContainerConfigurationService testImpl = new ContainerConfigurationServiceImpl(mock(ConfigurationService));
        DeploymentConfiguration dc = new DeploymentConfiguration();
        dc.setReadTimeout(25000);
        dc.setConnectionTimeout(25000);
        dc.setProxyThreadPool(15);
        assertThat(testImpl.doesContainDepricatedConfigs(dc),equalTo(true));
    }


}
