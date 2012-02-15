package com.rackspace.papi.commons.util.http.header;

import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

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

         assertEquals("Should choose highest value quality media type", "application/xml", QualityFactorUtility.choosePreferedHeaderValue(headerValues).getValue());
      }

      @Test
      public void shouldHandleNullHeaderValueObjects() {
         final List<HeaderValue> headerValues = new HeaderFieldParser(MEDIA_TYPE).parse();
         headerValues.add(null);

         assertEquals("Should choose highest value quality media type", "application/xml", QualityFactorUtility.choosePreferedHeaderValue(headerValues).getValue());
      }

      @Test
      public void shouldReturnNullWhenGivenNull() {
         assertNull("Quality factor utility must return null when an empty header value list is given", QualityFactorUtility.choosePreferedHeaderValue(null));
      }

      @Test
      public void shouldReturnNullWhenGivenEmptyListOfHeaderValues() {
         assertTrue("Quality factor utility must return null when an empty header value list is given", QualityFactorUtility.choosePreferedHeaderValue(Collections.EMPTY_LIST) == null);
      }

      @Test
      public void shouldChooseHighestQualityHeaderValue() {
         final List<HeaderValue> headerValues = new HeaderFieldParser(MEDIA_TYPE).parse();

         assertEquals("Should choose highest value quality media type", "application/xml", QualityFactorUtility.choosePreferedHeaderValue(headerValues).getValue());
      }
   }

   public static class WhenChoosingHeaderValuesWithMatchingQuality {

      private static final String MEDIA_TYPE_WITH_DUPLICATE_QUALITY = "application/vnd.rackspace.services.a+xml; q=0.8, application/json; q=0.9, application/xml; q=0.9, */*; q=0.1";

      @Test
      public void shouldChooseHeaderValueByLexicographicOrderWhenQualitiesMatch() {
         final List<HeaderValue> headerValues = new HeaderFieldParser(MEDIA_TYPE_WITH_DUPLICATE_QUALITY).parse();

         assertEquals("Should choose highest value quality media type", "application/xml", QualityFactorUtility.choosePreferedHeaderValue(headerValues).getValue());
      }
   }
}
