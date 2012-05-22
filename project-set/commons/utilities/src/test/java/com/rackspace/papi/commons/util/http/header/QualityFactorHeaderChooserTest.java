package com.rackspace.papi.commons.util.http.header;

import com.rackspace.papi.commons.util.http.media.MediaRangeParser;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MimeType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class QualityFactorHeaderChooserTest {

   @Ignore
   public static class TestParent {

      protected QualityFactorHeaderChooser qualityFactorHeaderChooser;
      protected HeaderValue defaultHeaderValue;

      @Before
      public void beforeAny() {
         defaultHeaderValue = new MediaType(MimeType.APPLICATION_JSON.getMimeType(), MimeType.APPLICATION_JSON, -1);
         qualityFactorHeaderChooser = new QualityFactorHeaderChooser(defaultHeaderValue);
      }
   }

   public static class WhenChoosingHeaderValuesByQuality extends TestParent {

      private static final String MEDIA_TYPE = "application/vnd.rackspace.services.a+xml; q=0.8, application/json; q=0.5, application/xml; q=0.9, */*; q=0.1";

      @Test
      public void shouldCastHeaderValueType() {
         QualityFactorHeaderChooser<MediaType> mediaTypeChooser = new QualityFactorHeaderChooser<MediaType>(new MediaType(MimeType.WILDCARD, -1));
         final List<MediaType> headerValues = new MediaRangeParser(MEDIA_TYPE).parse();

         assertEquals("Should choose highest value quality media type", MimeType.APPLICATION_XML, mediaTypeChooser.choosePreferredHeaderValue(headerValues).getMimeType());
      }

      @Test
      public void shouldHandleNullHeaderValues() {
         final List<HeaderValue> headerValues = new HeaderFieldParser(MEDIA_TYPE).parse();
         headerValues.add(new HeaderValueImpl(null, -1));

         assertEquals("Should choose highest value quality media type", "application/xml", qualityFactorHeaderChooser.choosePreferredHeaderValue(headerValues).getValue());
      }

      @Test
      public void shouldHandleNullHeaderValueInPopulatedList() {
         final List<HeaderValue> headerValues = new HeaderFieldParser(MEDIA_TYPE).parse();
         headerValues.add(null);

         assertEquals("Should choose highest value quality media type", "application/xml", qualityFactorHeaderChooser.choosePreferredHeaderValue(headerValues).getValue());
      }

      @Test
      public void shouldReturnDefaultWhenGivenNull() {
         assertEquals("Quality factor utility must return default when an empty header value is given", defaultHeaderValue, qualityFactorHeaderChooser.choosePreferredHeaderValue(null));
      }

      @Test
      public void shouldReturnDefaultWhenGivenEmptyListOfHeaderValues() {
         assertEquals("Quality factor utility must return default when an empty header value list is given", defaultHeaderValue, qualityFactorHeaderChooser.choosePreferredHeaderValue(Collections.EMPTY_LIST));
      }

      @Test
      public void shouldChooseHighestQualityHeaderValue() {
         final List<HeaderValue> headerValues = new HeaderFieldParser(MEDIA_TYPE).parse();

         assertEquals("Should choose highest value quality media type", "application/xml", qualityFactorHeaderChooser.choosePreferredHeaderValue(headerValues).getValue());
      }
   }

   public static class WhenChoosingHeaderValuesWithMatchingQuality extends TestParent {

      private static final String MEDIA_TYPE_WITH_DUPLICATE_QUALITY = "application/vnd.rackspace.services.a+xml; q=0.8, application/json; q=0.9, application/xml; q=0.9, */*; q=0.1";

      @Test
      public void shouldChooseFirstHeaderOfHighestQuality() {
         final List<HeaderValue> headerValues = new HeaderFieldParser(MEDIA_TYPE_WITH_DUPLICATE_QUALITY).parse();

         assertEquals("Should choose highest value quality media type", "application/json", qualityFactorHeaderChooser.choosePreferredHeaderValue(headerValues).getValue());
      }
   }
   
      public static class WhenChoosingAllHeaderValuesByQuality extends TestParent {

      private static final String GROUPS_WITH_UNIQUE_HIGHEST_QUALITY = "group1;q=0.1,group2;q=0.1,highest;q=1.0,lowest;q=0.001";
      private static final String GROUPS_WITH_DUPLICATE_HIGHEST_QUALITY = "group1;q=0.1,group2;q=0.1,highest1;q=1.0,highest2;q=1.0,lowest;q=0.001";
      private static final String GROUPS_WITHOUT_QUALITY = "group1,group2,highest1,highest2,lowest";

      @Test
      public void shouldReturnDefaultWhenGivenNull() {
         final List<HeaderValue> preferred = qualityFactorHeaderChooser.choosePreferredHeaderValues(null);
         
         assertEquals("Quality factor utility should return a list with the default when null header list given", 1, preferred.size());
         assertEquals("Quality factor utility should return a list with the default when null header list given", defaultHeaderValue, preferred.get(0));
      }

      @Test
      public void shouldFindHighestQuality() {
         final List<HeaderValue> headerValues = new HeaderFieldParser(GROUPS_WITH_UNIQUE_HIGHEST_QUALITY).parse();
         final List<HeaderValue> actual = qualityFactorHeaderChooser.choosePreferredHeaderValues(headerValues);

         final String expectedHeader = "highest";

         assertEquals("Should find highest quality header", 1, actual.size());
         assertEquals("Should find header value associated with highest quality", expectedHeader, actual.get(0).getValue());
      }

      @Test
      public void shouldHandleGroupsWithoutQuality() {
         final List<HeaderValue> headerValues = new HeaderFieldParser(GROUPS_WITHOUT_QUALITY).parse();
         List<HeaderValue> actual = qualityFactorHeaderChooser.choosePreferredHeaderValues(headerValues);
         final int expectedSize = 5;

         assertEquals("Should handle groups without quality", expectedSize, actual.size());
      }

      @Test
      public void shouldFindAllHighestQualityValues() {
         final List<HeaderValue> headerValues = new HeaderFieldParser(GROUPS_WITH_DUPLICATE_HIGHEST_QUALITY).parse();
         List<HeaderValue> actual = qualityFactorHeaderChooser.choosePreferredHeaderValues(headerValues);
         final int expectedSize = 2;
         final String expectedHeader = "highest";

         assertEquals("Should find highest quality header", expectedSize, actual.size());

         // ensure that we found all of the headers associated with the highest value
         for (int i = 1; i <= 2; i++) {
            boolean found = false;
            for (HeaderValue value : actual) {
               if (value.getValue().equals(expectedHeader + 1)) {
                  found = true;
               }
            }

            assertTrue("Should find header value associated with highest quality", found);
         }
      }
   }
}
