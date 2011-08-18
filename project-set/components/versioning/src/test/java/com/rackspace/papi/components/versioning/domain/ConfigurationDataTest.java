package com.rackspace.papi.components.versioning.domain;

import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;
import com.rackspace.papi.components.versioning.schema.VersionChoice;
import com.rackspace.papi.components.versioning.testhelpers.ConfigurationDataCreator;
import com.rackspace.papi.components.versioning.testhelpers.HttpServletRequestMockFactory;
import com.rackspace.papi.commons.util.http.HttpRequestInfoImpl;
import com.rackspace.papi.commons.util.http.UniformResourceInfo;
import com.rackspace.papi.model.Host;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static com.rackspace.papi.components.versioning.testhelpers.TestConsts.SERVICE_ROOT_HREF;
import static com.rackspace.papi.components.versioning.testhelpers.TestConsts.REQUEST_URI;
import static com.rackspace.papi.components.versioning.testhelpers.TestConsts.REQUEST_URL_WITHOUT_VERSION;
import static com.rackspace.papi.components.versioning.testhelpers.TestConsts.REQUEST_URL_WITH_VERSION;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 6/7/11
 * Time: 10:46 AM
 */
@RunWith(Enclosed.class)
public class ConfigurationDataTest {

    public static class WhenGettingServiceMappingsAsList {
        private ConfigurationData configData;
        private UniformResourceInfo requestResourceInfo;

        @Before
        public void setup() {
            requestResourceInfo
                    = new HttpRequestInfoImpl(HttpServletRequestMockFactory.create(REQUEST_URI, REQUEST_URL_WITHOUT_VERSION, ""));

            configData = new ConfigurationData(SERVICE_ROOT_HREF,
                    ConfigurationDataCreator.createConfiguredHosts(3), ConfigurationDataCreator.createConfiguredMappings(2));
        }

        @Test
        public void shouldReturnListWithExpectedNumberOfElements() {
            assertEquals(2, configData.versionChoicesAsList(requestResourceInfo).getVersion().size());
        }

        @Test
        public void shouldIncludeVersionNumber() {
            VersionChoice versionChoice = configData.versionChoicesAsList(requestResourceInfo).getVersion().get(0);
            String href = versionChoice.getLink().get(0).getHref();

            assertEquals("http://rackspacecloud.com/v1.0/the/target/resource/", href);
        }
    }

    public static class WhenCheckingIfMatchesHostUrl {
        private UniformResourceInfo requestResourceInfo;

        @Before
        public void setup() {
            requestResourceInfo = null;
        }

        @Test
        public void shouldBeTrueIfMatchesBeginning() {
            requestResourceInfo
                    = new HttpRequestInfoImpl(HttpServletRequestMockFactory.create(REQUEST_URI, REQUEST_URL_WITH_VERSION, ""));

            assertTrue(ConfigurationData.matchesHostUrl(requestResourceInfo, SERVICE_ROOT_HREF));
        }

        @Test
        public void shouldRequireMoreThanProtocol() {
            requestResourceInfo
                    = new HttpRequestInfoImpl(HttpServletRequestMockFactory.create(REQUEST_URI, REQUEST_URL_WITH_VERSION, ""));

            assertFalse("non-secure", ConfigurationData.matchesHostUrl(requestResourceInfo, "http://"));
            assertFalse("secure", ConfigurationData.matchesHostUrl(requestResourceInfo, "https://"));
        }

        @Test
        public void shouldBeFalseIfBeginningDoesNotMatch() {
            requestResourceInfo
                    = new HttpRequestInfoImpl(HttpServletRequestMockFactory.create(REQUEST_URI, REQUEST_URL_WITH_VERSION, ""));

            assertFalse(ConfigurationData.matchesHostUrl(requestResourceInfo, "http://mosso.com"));
        }

        @Test
        public void shouldBeFalseIfHostRefIsBlank() {
            requestResourceInfo
                    = new HttpRequestInfoImpl(HttpServletRequestMockFactory.create(REQUEST_URI, REQUEST_URL_WITH_VERSION, ""));

            assertFalse(ConfigurationData.matchesHostUrl(requestResourceInfo, " "));
        }

        @Test
        public void shouldBeFalseIfHostRefIsNull() {
            requestResourceInfo
                    = new HttpRequestInfoImpl(HttpServletRequestMockFactory.create(REQUEST_URI, REQUEST_URL_WITH_VERSION, ""));
            String undefined = null;

            assertFalse(ConfigurationData.matchesHostUrl(requestResourceInfo, undefined));
        }

        @Test(expected=IllegalArgumentException.class)
        public void shouldThrowExceptionIfRequestUrlIsNull() {
            requestResourceInfo = null;

            ConfigurationData.matchesHostUrl(requestResourceInfo, "");
        }
    }

    public static class WhenMappingOriginHostFromVersionId {
        private ConfigurationData configData;
        private Map<String, ServiceVersionMapping> configuredMappings;
        private Map<String, Host> configuredHosts;
        private Host host;

        @Before
        public void setup() {
            configuredHosts = new HashMap<String, Host>();
            host = new Host();
            host.setHref(REQUEST_URL_WITH_VERSION);
            configuredHosts.put("hostId2", host);

            ServiceVersionMapping mapping;
            configuredMappings = new HashMap<String, ServiceVersionMapping>();
            mapping = new ServiceVersionMapping();
            mapping.setName("service1");
            mapping.setPpHostId("hostId1");
            configuredMappings.put("v1.0", mapping);
            mapping = new ServiceVersionMapping();
            mapping.setName("service2");
            mapping.setPpHostId("hostId2");
            configuredMappings.put("v2.0", mapping);

            configData = new ConfigurationData(SERVICE_ROOT_HREF, configuredHosts, configuredMappings);
        }

        @Test
        public void shouldHaveExpectedNumberOfConfiguredMappings() {
            assertEquals(2, configData.getServiceMappings().size());
        }

        @Test
        public void shouldReturnSameInstanceOfMappedHost() {
            Host mappedHost = configData.mapOriginHostFromVersionId("v2.0");

            assertSame(host, mappedHost);
        }

        @Test
        public void shouldReturnNullIfHostIdNotFoundInConfiguredHosts() {
            assertNull(configData.mapOriginHostFromVersionId("v1.0"));
        }

        @Test
        public void shouldReturnNullIfMappingNotFound() {
            assertNull(configData.mapOriginHostFromVersionId("vX.0"));
        }
    }

    public static class WhenCreatingNewInstances {
        private static final Integer SERVICE_MAPPINGS_COUNT = 2;
        private ConfigurationData configData;
        private Map<String, Host> configuredHosts;
        private Map<String, ServiceVersionMapping> serviceMappings;

        @Before
        public void setup() {
            configuredHosts = ConfigurationDataCreator.createConfiguredHosts(3);
            serviceMappings = ConfigurationDataCreator.createConfiguredMappings(SERVICE_MAPPINGS_COUNT);
        }

        @Test
        public void shouldBeAbleToProduceVersionIdList() {
            configData = new ConfigurationData("http://rackspacecloud.com/", configuredHosts, serviceMappings);

            Set<String> versionIds;

            versionIds = configData.getVersionIds();

            assertEquals("should have same number as service mappings", 2, versionIds.size());
            assertTrue("should have first id", versionIds.contains("v1.0"));
            assertTrue("should have second id", versionIds.contains("v1.1"));
        }

        @Test
        public void shouldAddEndingSlashOnUrlIfNotPresent() {
            configData = new ConfigurationData("http://rackspacecloud.com", configuredHosts, serviceMappings);

            assertEquals("http://rackspacecloud.com/", configData.getServiceRootHref());
        }

        @Test
        public void shouldNotAddEndingSlashOnUrlIfAlreadyPresent() {
            configData = new ConfigurationData("http://rackspacecloud.com/", configuredHosts, serviceMappings);

            assertEquals("http://rackspacecloud.com/", configData.getServiceRootHref());
        }
    }

    public static class WhenCheckingIfShouldReturnVersionChoices {
        private ConfigurationData configData;
        private UniformResourceInfo uniformResourceInfo;

        @Before
        public void setup() {
            configData = new ConfigurationData("http://rackspacecloud.com/", null, null);
            uniformResourceInfo = mock(UniformResourceInfo.class);
        }

        @Test
        public void shouldReturnTrueIfParameterUrlStartsWithRootContextAndHasResourceInfo() {
            when(uniformResourceInfo.getUrl()).thenReturn("http://rackspacecloud.com/ducks");

            assertTrue(configData.isRequestForVersionChoices(uniformResourceInfo));
        }

        @Ignore(value = "not complete yet!")
        @Test
        public void shouldReturnFalseIfParameterUrlContainsVersionInfo() {
            when(uniformResourceInfo.getUrl()).thenReturn("http://rackspacecloud.com/v1.0/ducks");

            assertFalse(configData.isRequestForVersionChoices(uniformResourceInfo));
        }

        @Test
        public void shouldReturnTrueIfParameterUrlMatchesRootContext() {
            when(uniformResourceInfo.getUrl()).thenReturn("http://rackspacecloud.com/");

            assertTrue(configData.isRequestForVersionChoices(uniformResourceInfo));
        }

        @Test
        public void shouldReturnFalseIfParameterUrlDoesNotStartWithRootContext() {
            when(uniformResourceInfo.getUrl()).thenReturn("http://rackspaceclouds.com/v1.0/ducks");

            assertFalse(configData.isRequestForVersionChoices(uniformResourceInfo));
        }

        @Test(expected=NullPointerException.class)
        public void shouldThrowExceptionIfRequestUrlIsNull() {
            when(uniformResourceInfo.getUrl()).thenReturn(null);

            configData.isRequestForVersionChoices(uniformResourceInfo);
        }
    }

    public static class WhenGettingOriginToRouteTo {
        @Test
        public void shouldFindHostUsingUriVariantMatchingEvenIfVersionInRequest() {
            String uri = "v1.0/the/target/resource/";
            String url = SERVICE_ROOT_HREF + uri;
            String acceptHeader = "application/vnd.vendor.service-v1.1+xml";

            ConfigurationData configData = new ConfigurationData(SERVICE_ROOT_HREF,
                    ConfigurationDataCreator.createConfiguredHosts(2), ConfigurationDataCreator.createConfiguredMappings(2));

            Host actual = configData.getOriginToRouteTo(new HttpRequestInfoImpl(HttpServletRequestMockFactory.create(uri, url, acceptHeader)));

            assertEquals("service-v1.0", actual.getId());
        }

        @Test
        public void shouldFindHostThroughAcceptHeaderIfVersionNotFoundInVariant() {
            String uri = "the/target/resource/";
            String url = SERVICE_ROOT_HREF + uri;
            String acceptHeader = "application/vnd.vendor.service-v1.1+xml";

            ConfigurationData configData = new ConfigurationData(SERVICE_ROOT_HREF,
                    ConfigurationDataCreator.createConfiguredHosts(2), ConfigurationDataCreator.createConfiguredMappings(2));

            Host actual = configData.getOriginToRouteTo(new HttpRequestInfoImpl(HttpServletRequestMockFactory.create(uri, url, acceptHeader)));

            assertEquals("service-v1.1", actual.getId());
        }

        @Test
        public void shouldFindHostThroughAcceptHeaderWithParameters() {
            String uri = "the/target/resource/";
            String url = SERVICE_ROOT_HREF + uri;
            String acceptHeader = "application/vnd.vendor.service; y=xml; x=v1.1";

            ConfigurationData configData = new ConfigurationData(SERVICE_ROOT_HREF,
                    ConfigurationDataCreator.createConfiguredHosts(2), ConfigurationDataCreator.createConfiguredMappingsWithAcceptParameters(2));

            Host actual = configData.getOriginToRouteTo(new HttpRequestInfoImpl(HttpServletRequestMockFactory.create(uri, url, acceptHeader)));

            assertEquals("service-v1.1", actual.getId());
        }

        @Test
        public void shouldReturnNullIfNoVersionInfoInVariantOrRequest() {
            String uri = "the/target/resource/";
            String url = SERVICE_ROOT_HREF + uri;
            String acceptHeader = "application/vnd.vendor.service+xml";

            ConfigurationData configData = new ConfigurationData(SERVICE_ROOT_HREF,
                    ConfigurationDataCreator.createConfiguredHosts(2), ConfigurationDataCreator.createConfiguredMappings(2));

            Host actual = configData.getOriginToRouteTo(new HttpRequestInfoImpl(HttpServletRequestMockFactory.create(uri, url, acceptHeader)));

            assertNull(actual);
        }


    }
}
