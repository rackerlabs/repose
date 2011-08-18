package com.rackspace.papi.components.versioning;

import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;
import com.rackspace.papi.components.versioning.testhelpers.TestConsts;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zinic
 */
@RunWith(Enclosed.class)
public class VersioningTaggerTest {
    public static class WhenReturningMultipleChoices {
        private MediaType acceptType;
        private VersioningTagger versioningTagger;
        private Map<String, ServiceVersionMapping> configuredMappings;

        @Before
        public void setup() {
            acceptType = MediaType.APPLICATION_JSON;

            ServiceVersionMapping mapping;
            configuredMappings = new HashMap<String, ServiceVersionMapping>();
            mapping = new ServiceVersionMapping();
            mapping.setName("mapping1");
            mapping.setPpHostId("hostId1");
            configuredMappings.put("mapping1", mapping);

            versioningTagger = new VersioningTagger(configuredMappings, null, TestConsts.SERVICE_ROOT_HREF);
        }

        @Test
        public void should() {
            
        }
    }
}
