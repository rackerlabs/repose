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
package org.openrepose.filters.urinormalization.normalizer;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.filters.urinormalization.config.HttpUriParameterList;
import org.openrepose.filters.urinormalization.config.UriParameter;

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
