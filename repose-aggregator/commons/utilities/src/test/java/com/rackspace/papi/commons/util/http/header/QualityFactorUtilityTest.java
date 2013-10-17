package com.rackspace.papi.commons.util.http.header;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class QualityFactorUtilityTest {

   public static class WhenChoosingHeaderValuesByQuality {

      private static final String MEDIA_TYPE = "application/vnd.rackspace.services.a+xml; q=0.8, application/json; q=0.5, application/xml; q=0.9, */*; q=0.1";

      @Test
      public void shouldHandleNullHeaderValues() {
         final List<HeaderValue> headerValues = new HeaderFieldParser(MEDIA_TYPE).parse();
         headerValues.add(new HeaderValueImpl(null, -1));

         assertEquals("Should choose highest value quality media type", "application/xml", QualityFactorUtility.choosePreferredHeaderValue(headerValues).getValue());
      }

      @Test
      public void shouldHandleNullHeaderValueObjects() {
         final List<HeaderValue> headerValues = new HeaderFieldParser(MEDIA_TYPE).parse();
         headerValues.add(null);

         assertEquals("Should choose highest value quality media type", "application/xml", QualityFactorUtility.choosePreferredHeaderValue(headerValues).getValue());
      }

      @Test
      public void shouldReturnNullWhenGivenNull() {
         assertNull("Quality factor utility must return null when an empty header value list is given", QualityFactorUtility.choosePreferredHeaderValue(null));
      }

      @Test
      public void shouldReturnNullWhenGivenEmptyListOfHeaderValues() {
         assertTrue("Quality factor utility must return null when an empty header value list is given", QualityFactorUtility.choosePreferredHeaderValue(Collections.EMPTY_LIST) == null);
      }

      @Test
      public void shouldChooseHighestQualityHeaderValue() {
         final List<HeaderValue> headerValues = new HeaderFieldParser(MEDIA_TYPE).parse();

         assertEquals("Should choose highest value quality media type", "application/xml", QualityFactorUtility.choosePreferredHeaderValue(headerValues).getValue());
      }
   }

   public static class WhenChoosingHeaderValuesWithMatchingQuality {

      private static final String MEDIA_TYPE_WITH_DUPLICATE_QUALITY = "application/vnd.rackspace.services.a+xml; q=0.8, application/json; q=0.9, application/xml; q=0.9, */*; q=0.1";

      @Test
      public void shouldChooseHeaderValueByLexicographicOrderWhenQualitiesMatch() {
         final List<HeaderValue> headerValues = new HeaderFieldParser(MEDIA_TYPE_WITH_DUPLICATE_QUALITY).parse();

         assertEquals("Should choose highest value quality media type", "application/xml", QualityFactorUtility.choosePreferredHeaderValue(headerValues).getValue());
      }
   }
   
   public static class WhenChoosingAllHeaderValuesByQuality {
      private static final String GROUPS_WITH_UNIQUE_HIGHEST_QUALITY = "group1;q=0.1,group2;q=0.1,highest;q=1.0,lowest;q=0.001";
      private static final String GROUPS_WITH_DUPLICATE_HIGHEST_QUALITY = "group1;q=0.1,group2;q=0.1,highest1;q=1.0,highest2;q=1.0,lowest;q=0.001";
      private static final String GROUPS_WITHOUT_QUALITY = "group1,group2,highest1,highest2,lowest";
      
      @Test
      public void shouldReturnEmptyListWhenGivenNull() {
         assertEquals("Quality factor utility should return empty list when null header list given", 0, QualityFactorUtility.choosePreferredHeaderValues(null).size());
      }
      
      @Test
      public void shouldFindHighestQuality() {
         final List<HeaderValue> headerValues = new HeaderFieldParser(GROUPS_WITH_UNIQUE_HIGHEST_QUALITY).parse();
         List<HeaderValue> actual = QualityFactorUtility.choosePreferredHeaderValues(headerValues);
         final int expectedSize = 1;
         final String expectedHeader = "highest";
         
         assertEquals("Should find highest quality header", expectedSize, actual.size());
         assertEquals("Should find header value associated with highest quality", expectedHeader, actual.get(0).getValue());
      }

      @Test
      public void shouldHandleGroupsWithoutQuality() {
         final List<HeaderValue> headerValues = new HeaderFieldParser(GROUPS_WITHOUT_QUALITY).parse();
         List<HeaderValue> actual = QualityFactorUtility.choosePreferredHeaderValues(headerValues);
         final int expectedSize = 5;
         
         assertEquals("Should handle groups without quality", expectedSize, actual.size());
      }

      @Test
      public void shouldFindAllHighestQualityValues() {
         final List<HeaderValue> headerValues = new HeaderFieldParser(GROUPS_WITH_DUPLICATE_HIGHEST_QUALITY).parse();
         List<HeaderValue> actual = QualityFactorUtility.choosePreferredHeaderValues(headerValues);
         final int expectedSize = 2;
         final String expectedHeader = "highest";
         
         assertEquals("Should find highest quality header", expectedSize, actual.size());
         
         // ensure that we found all of the headers associated with the highest value
         for (int i = 1; i <= 2; i++) {
            boolean found = false;
            for (HeaderValue value: actual) {
               if (value.getValue().equals(expectedHeader + 1)) {
                  found = true;
               }
            }
            
            assertTrue("Should find header value associated with highest quality", found);
         }
      }
   }
}
