package com.rackspace.papi.components.versioning;

import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;
import com.rackspace.papi.components.versioning.domain.ConfigurationData;
import com.rackspace.papi.components.versioning.schema.VersionChoiceList;
import com.rackspace.papi.components.versioning.testhelpers.ConfigurationDataCreator;
import com.rackspace.papi.components.versioning.testhelpers.HttpServletRequestMockFactory;
import com.rackspace.papi.components.versioning.testhelpers.XmlTestHelper;
import com.rackspace.papi.commons.util.http.HttpRequestInfo;
import com.rackspace.papi.commons.util.http.HttpRequestInfoImpl;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.io.FilePathReaderImpl;
import com.rackspace.papi.commons.util.io.FileReader;
import com.rackspace.papi.model.Host;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.rackspace.papi.components.versioning.testhelpers.TestConsts.*;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: Apr 26, 2011
 * Time: 2:46:19 PM
 */
@RunWith(Enclosed.class)
public class VersioningTaggerHelperTest {

    private static final String REQUEST_URL = "http://rackspacecloud.com/v1.0/the/target/resource/";
    private static final String HOST_HREF = REQUEST_URL;

    public static class WhenReturningMultipleChoices {

        private MediaType acceptType;
        private Map<String, ServiceVersionMapping> configuredMappings;
        private VersioningTaggerHelper helper;
        private HttpRequestInfo requestResourceInfo;
        private FilterDirector result;
        private FileReader choicesJsonFileReader;

        @Before
        public void setup() {
            choicesJsonFileReader = new FilePathReaderImpl("/META-INF/schema/examples/json/choices2.json");

            acceptType = MediaType.APPLICATION_JSON;
            requestResourceInfo = new HttpRequestInfoImpl(HttpServletRequestMockFactory.create(REQUEST_URI, REQUEST_URL, ""));

            configuredMappings = ConfigurationDataCreator.createConfiguredMappings(2);

            ConfigurationData configData = new ConfigurationData(SERVICE_ROOT_HREF, null, configuredMappings);
            helper = new VersioningTaggerHelper(configData);

            result = helper.returnMultipleChoices(requestResourceInfo, acceptType);
        }

        @Test
        public void shouldGenerateResponseMessageBody() throws IOException {
            String expected, actual;

            expected = choicesJsonFileReader.read();

            actual = result.getResponseMessageBody();

            Assert.assertEquals(expected, actual);
        }

        @Test
        public void shouldAlwaysReturnSameDelegatedStatus() {
            Assert.assertEquals(HttpStatusCode.MULTIPLE_CHOICES, result.getResponseStatus());
        }

        @Test
        public void shouldAlwaysReturnSameDelegatedAction() {
            Assert.assertEquals(FilterAction.RETURN, result.getFilterAction());
        }
    }

    public static class WhenGettingVersioningInformationRequestWithNoVersionMatch {

        private VersioningTaggerHelper helper;
        private Map<String, ServiceVersionMapping> configuredMappings;
        private Map<String, Host> configuredHosts;
        private JAXBElement result;
        private HttpRequestInfo requestResourceInfo;

        @Before
        public void setup() {
            result = null;

            requestResourceInfo = new HttpRequestInfoImpl(HttpServletRequestMockFactory.create(REQUEST_URI, REQUEST_URL_WITHOUT_VERSION, ""));

            configuredHosts = ConfigurationDataCreator.createConfiguredHosts(1);
            configuredMappings = ConfigurationDataCreator.createConfiguredMappings(2);

            ConfigurationData configData = new ConfigurationData(SERVICE_ROOT_HREF, configuredHosts, configuredMappings);
            helper = new VersioningTaggerHelper(configData);

            result = helper.getVersioningInformation(requestResourceInfo);
        }

        @Test
        public void shouldReturnJAXBElement() {
            assertNotNull(result);
        }

        @Test
        public void shouldReturnVersionChoiceList() {
            assertTrue(result.getValue().toString().contains("VersionChoiceList"));
        }

        @Test
        public void shouldReturnTwoVersionChoices() {
            VersionChoiceList choiceList = (VersionChoiceList) result.getValue();
            assertEquals(2, choiceList.getVersion().size());
        }
    }

    public static class WhenTransformVersioningInformationRequest {

        private VersioningTaggerHelper helper;
        private MediaType acceptType;
        private Map<String, ServiceVersionMapping> configuredMappings;
        private Map<String, Host> configuredHosts;
        private Host host;

        @Before
        public void setup() {
            configuredHosts = new HashMap<String, Host>();
            host = new Host();
            host.setId("hostId1");
            host.setHref(HOST_HREF);
            configuredHosts.put("hostId1", host);

            host = new Host();
            host.setId("hostId2");
            host.setHref(HOST_HREF);
            configuredHosts.put("hostId2", host);

            ServiceVersionMapping mapping;
            configuredMappings = new HashMap<String, ServiceVersionMapping>();

            mapping = new ServiceVersionMapping();
            mapping.setId("v1.0");
            mapping.setName("service1");
            mapping.setPpHostId("hostId1");
            configuredMappings.put("v1.0", mapping);

            mapping = new ServiceVersionMapping();
            mapping.setId("v2.0");
            mapping.setName("service2");
            mapping.setPpHostId("hostId2");
            configuredMappings.put("v2.0", mapping);

            acceptType = MediaType.APPLICATION_JSON;

            ConfigurationData configData = new ConfigurationData(SERVICE_ROOT_HREF, configuredHosts, configuredMappings);
            helper = new VersioningTaggerHelper(configData);
        }

        @Test
        public void shouldReturnTransformedMatchingMapping() {
            final HttpServletRequest request = HttpServletRequestMockFactory.create("/v1.0" + REQUEST_URI, HOST_HREF, "");

            final String expected = "{ \"version\" :{\"id\" : \"v1.0\"}}";
            final String actual = helper.transformVersioningInformationRequest(new HttpRequestInfoImpl(request), acceptType);

            assertEquals(expected, actual);
        }
    }

    public static class WhenTransformVersioningInformationRequestAndTargetVersionHostIsNotFound {

        private VersioningTaggerHelper helper;
        private MediaType acceptType;
        private Map<String, ServiceVersionMapping> configuredMappings;
        private Map<String, Host> configuredHosts;
        private FileReader versionsJsonFileReader;

        @Before
        public void setup() {
            versionsJsonFileReader = new FilePathReaderImpl("/META-INF/schema/examples/json/versions2.json");
            configuredHosts = new HashMap<String, Host>();

            configuredMappings = ConfigurationDataCreator.createConfiguredMappings(2);

            acceptType = MediaType.APPLICATION_JSON;

            ConfigurationData configData = new ConfigurationData(SERVICE_ROOT_HREF, configuredHosts, configuredMappings);
            helper = new VersioningTaggerHelper(configData);
        }

        @Test
        public void shouldReturnMultipleChoices() throws IOException {
            String expected, actual;

            expected = versionsJsonFileReader.read();

            HttpServletRequest request = HttpServletRequestMockFactory.create(REQUEST_URI, SERVICE_ROOT_HREF, "");
            actual = helper.transformVersioningInformationRequest(new HttpRequestInfoImpl(request), acceptType);

            assertEquals(expected, actual);
        }

        @Test
        public void shouldReturnExpectedXml() throws JAXBException, SAXException {
            String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<versions xmlns=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\">\n"
                    + "    <version status=\"CURRENT\" id=\"v1.0\">\n"
                    + "        <media-types>\n"
                    + "            <media-type type=\"application/vnd.vendor.service-v1.0+xml\" base=\"application/xml\"/>\n"
                    + "            <media-type type=\"application/vnd.vendor.service-v1.0+json\" base=\"application/json\"/>\n"
                    + "        </media-types>\n"
                    + "        <ns2:link href=\"http://rackspacecloud.com/v1.0/the/target/resource/\" rel=\"self\"/>\n"
                    + "    </version>\n"
                    + "    <version status=\"CURRENT\" id=\"v1.1\">\n"
                    + "        <media-types>\n"
                    + "            <media-type type=\"application/vnd.vendor.service-v1.1+xml\" base=\"application/xml\"/>\n"
                    + "            <media-type type=\"application/vnd.vendor.service-v1.1+json\" base=\"application/json\"/>\n"
                    + "        </media-types>\n"
                    + "        <ns2:link href=\"http://rackspacecloud.com/v1.1/the/target/resource/\" rel=\"self\"/>\n"
                    + "    </version>\n"
                    + "</versions>\n";
            System.out.println(expected);

            HttpRequestInfo requestResourceInfo = mock(HttpRequestInfo.class);
            when(requestResourceInfo.getUri()).thenReturn(REQUEST_URI);
            when(requestResourceInfo.getUrl()).thenReturn(SERVICE_ROOT_HREF);

            JAXBElement versioningInfo = helper.getVersioningInformation(requestResourceInfo);

            assertEquals(expected, XmlTestHelper.getXmlString(versioningInfo, Boolean.TRUE, XmlTestHelper.getVersioningSchemaInfo()));
        }

        @Test
        public void shouldReturnNullIfShouldNotExplainVersionList() {
            String actual;

            HttpServletRequest request = HttpServletRequestMockFactory.create(REQUEST_URI, "", "");
            actual = helper.transformVersioningInformationRequest(new HttpRequestInfoImpl(request), acceptType);

            assertNull(actual);
        }
    }

    public static class WhenGettingOriginToRouteTo {

        private VersioningTaggerHelper helper;
        private HttpServletRequest request;
        private Map<String, ServiceVersionMapping> configuredMappings;
        private Map<String, Host> configuredHosts;
        private Host host;

        @Before
        public void setup() {
            request = HttpServletRequestMockFactory.create(REQUEST_URI, HOST_HREF, "");

            configuredHosts = ConfigurationDataCreator.createConfiguredHosts(1);
            host = configuredHosts.get("service-v1.0");

            configuredMappings = ConfigurationDataCreator.createConfiguredMappings(2);

            ConfigurationData configData =
                    new ConfigurationData(SERVICE_ROOT_HREF, configuredHosts, configuredMappings);
            helper = new VersioningTaggerHelper(configData);

        }

        @Test
        public void shouldFindHostThroughAcceptHeaderIfIsInRequest() {
            Host expected, actual;

            expected = host;

            actual = helper.getOriginToRouteTo(new HttpRequestInfoImpl(request));

            assertSame(expected, actual);
            assertEquals("href", "http://rackspacecloud.com/v1.0", actual.getHref());
        }

        @Test
        public void shouldFindHostUsingUriVariantMatchingIfVersionNotFoundThroughRequest() {
            Host expected, actual;

            expected = host;

            actual = helper.getOriginToRouteTo(new HttpRequestInfoImpl(request));

            assertSame(expected, actual);
        }
    }
}
