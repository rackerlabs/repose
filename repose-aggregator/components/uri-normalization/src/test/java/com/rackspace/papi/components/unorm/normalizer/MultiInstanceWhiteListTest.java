package com.rackspace.papi.components.unorm.normalizer;

import com.rackspace.papi.components.uri.normalization.config.HttpUriParameterList;
import com.rackspace.papi.components.uri.normalization.config.UriParameter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class MultiInstanceWhiteListTest {

   @Ignore
   public static class TestParent {

      protected MultiInstanceWhiteList whiteList;

      @Before
      public void beforeAll() {
         final HttpUriParameterList parameterList = new HttpUriParameterList();

         final UriParameter parameterA = new UriParameter();
         parameterA.setName("a");
         parameterA.setCaseSensitive(false);
         parameterA.setMultiplicity(2);

         parameterList.getParameter().add(parameterA);

         final UriParameter parameterB = new UriParameter();
         parameterB.setName("b");
         parameterB.setCaseSensitive(true);
         parameterB.setMultiplicity(4);

         parameterList.getParameter().add(parameterB);

         final UriParameter parameterC = new UriParameter();
         parameterC.setName("c");
         parameterC.setCaseSensitive(true);
         parameterC.setMultiplicity(0);

         parameterList.getParameter().add(parameterC);

         whiteList = new MultiInstanceWhiteList(parameterList);
      }
   }

   public static class WhenFilteringParameterMultiplicity extends TestParent {

      @Test
      public void shouldFilterParametersInWhiteList() {
         assertTrue("Should accept 'a'", whiteList.shouldAccept("a"));
         assertTrue("Should accept 'A'", whiteList.shouldAccept("A"));
         
         assertFalse("Should not accept third 'a'", whiteList.shouldAccept("a"));
         assertFalse("Should not accept 'test'", whiteList.shouldAccept("test"));
         assertFalse("Should not accept 'format'", whiteList.shouldAccept("format"));
         assertFalse("Should not accept 'B'", whiteList.shouldAccept("B"));
      }

      @Test
      public void shouldHonorUnlimitedMultiplicity() {
         for (int i = 0; i < 5000; i++) {
            assertTrue("Should accept 'a'", whiteList.shouldAccept("c"));
         }
      }
      
      @Test
      public void shouldNotAcceptAnythingWithNullParameter(){
         
         MultiInstanceWhiteList emptyWhiteList= new MultiInstanceWhiteList(null);
         
         assertFalse("Should not accept 'a'", emptyWhiteList.shouldAccept("a"));
         assertFalse("Should not accept 'test'", emptyWhiteList.shouldAccept("test"));
         assertFalse("Should not accept 'format'", emptyWhiteList.shouldAccept("format"));
      }
   }
}
