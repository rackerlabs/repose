package com.rackspace.papi.commons.util.http.media;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class MediaRangeParserTest {

   public static class WhenGettingMediaRangesFromAcceptHeader {

      @Test
      public void shouldParseStandardMediaRangeWithoutParameters() {
         String acceptHeader = "application/xml";

         List<MediaType> mediaRanges = new MediaRangeParser(acceptHeader).parse();

         Assert.assertEquals(MimeType.APPLICATION_XML, mediaRanges.get(0).getMimeType());
      }

      @Test
      public void shouldParseStandardMediaRangeWithParameters() {
         String acceptHeader = "application/xml; v=1.0; s=7; q=1";

         List<MediaType> mediaRanges = new MediaRangeParser(acceptHeader).parse();

         assertEquals(MimeType.APPLICATION_XML, mediaRanges.get(0).getMimeType());
         assertEquals(3, mediaRanges.get(0).getParameters().size());
      }

      @Test
      public void shouldParseAcceptHeaderWithVendorSpecificMediaRange() {
         String acceptHeader = "application/vnd.openstack.compute-v1.1+json";

         List<MediaType> mediaRanges = new MediaRangeParser(acceptHeader).parse();

         assertEquals(MimeType.APPLICATION_JSON, mediaRanges.get(0).getMimeType());
         assertEquals(acceptHeader, mediaRanges.get(0).getValue());
      }

      @Test
      public void shouldParseAcceptHeaderWithMultipleMediaRanges() {
         String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";

         List<MediaType> mediaRanges = new MediaRangeParser(acceptHeader).parse();

         assertEquals(4, mediaRanges.size());
      }
   }
}
