package com.rackspace.papi.service.reporting.jmx;

import com.rackspace.papi.service.reporting.ReportingService;
import com.rackspace.papi.service.reporting.impl.ReportingServiceImpl;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

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

         reportingService = new ReportingServiceImpl();
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
      public void whenRetrievingDestinationInfo() throws OpenDataException{
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
         
         assertTrue("Destination info contains total500s",data.get(0).containsKey("total500s"));
         assertTrue("Destination info contains total400s",data.get(0).containsKey("total400s"));
         assertTrue("Destination info contains unique destination id",data.get(0).containsKey("destinationId"));
      }
   }
}