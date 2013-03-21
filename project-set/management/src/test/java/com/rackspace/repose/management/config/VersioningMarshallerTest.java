package com.rackspace.repose.management.config;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.bind.JAXBElement;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Enclosed.class)
public class VersioningMarshallerTest {

    public static class TestParent {

        VersioningMarshaller versioningMarshaller;
        String configurationRoot;
        Object config;

        @Before
        public void setUp() throws Exception {
            versioningMarshaller = new VersioningMarshaller();
            configurationRoot = "project-set/management/src/test/resources/";
            config = new Integer(0);
        }

        @Test(expected = IllegalArgumentException.class)
        public void shouldThrowConfigErrorOnMarshal() throws Exception {
            versioningMarshaller.marshal(configurationRoot, config);
        }

        //@Test
        @Ignore //works local but not on build...
        public void shouldReturnJAXBElementWhenUnmarshaling() throws Exception {
            assertThat(versioningMarshaller.unmarshal(configurationRoot), is(instanceOf(JAXBElement.class)));
        }
    }
}
