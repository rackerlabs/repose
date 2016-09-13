/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.reporting.jmx;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.reporting.ReportingService;
import org.openrepose.core.services.reporting.impl.ReportingServiceImpl;
import org.openrepose.nodeservice.containerconfiguration.ContainerConfigurationService;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(Enclosed.class)
public class ReposeReportTest {

    public static class WhenRecordingEvents {

        private static final int REFRESH_SECONDS = 30;
        private ReportingService reportingService;
        private List<String> destinationIds = new ArrayList<String>();
        private ReposeReport report;

        @Before
        public void setUp() {
            destinationIds.add("id_1");
            destinationIds.add("id_2");
            destinationIds.add("id_7");

            reportingService = new ReportingServiceImpl(mock(ConfigurationService.class), mock(ContainerConfigurationService.class));
            reportingService.updateConfiguration(destinationIds, REFRESH_SECONDS);

            report = new ReposeReport(reportingService);
        }

        @Test
        public void whenRetrieving400sFromReport() {

            Long refresh = new Long("4333");
            reportingService.incrementReposeStatusCodeCount(400, refresh);
            reportingService.incrementReposeStatusCodeCount(401, refresh);
            reportingService.incrementReposeStatusCodeCount(403, refresh);
            reportingService.incrementReposeStatusCodeCount(404, refresh);
            reportingService.incrementReposeStatusCodeCount(415, refresh);


            assertEquals("5", report.getTotal400sReposeToClient());
        }

        @Test
        public void whenRetrieving500sFromReport() {

            Long refresh = new Long("4333");
            reportingService.incrementReposeStatusCodeCount(500, refresh);
            reportingService.incrementReposeStatusCodeCount(503, refresh);
            reportingService.incrementReposeStatusCodeCount(501, refresh);


            assertEquals("3", report.getTotal500sReposeToClient());
        }

        @Test
        public void whenRetrievingDestinationInfo() throws OpenDataException {
            Long refresh = new Long("4333");
            reportingService.recordServiceResponse("id_1", 400, refresh);
            reportingService.recordServiceResponse("id_1", 401, refresh);
            reportingService.recordServiceResponse("id_1", 403, refresh);
            reportingService.recordServiceResponse("id_1", 404, refresh);
            reportingService.recordServiceResponse("id_1", 415, refresh);
            reportingService.recordServiceResponse("id_7", 500, refresh);
            reportingService.recordServiceResponse("id_7", 503, refresh);
            reportingService.recordServiceResponse("id_7", 501, refresh);
            List<CompositeData> data = report.getDestinationInfo();

            assertTrue("Destination info contains total500s", data.get(0).containsKey("total500s"));
            assertTrue("Destination info contains total400s", data.get(0).containsKey("total400s"));
            assertTrue("Destination info contains unique destination id", data.get(0).containsKey("destinationId"));
        }
    }
}