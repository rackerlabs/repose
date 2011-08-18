/*
 *  Copyright 2010 Rackspace.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package com.rackspace.papi.commons.util.regex;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 *
 * 
 */
@RunWith(Enclosed.class)
public class SharedRegexMatcherTest {
   public static class WhenSplittingStrings {
      private final SharedRegexMatcher splitter = new SharedRegexMatcher(",");
      private final String simple = "1,2,3,4,5";
      private final String complex = ",,2,3,4,5,";

      @Test
      public void shouldProduceAccurateResultsWithSimpleStringSplitting() {
         final String[] expected = simple.split(",");
         final String[] actual = splitter.split(simple);

         assertEquals(expected.length, actual.length);
      }

      @Test
      public void shouldProduceAccurateResultsWithComplexStringSplitting() {
         final String[] expected = complex.split(",");
         final String[] actual = splitter.split(complex);

         assertEquals(expected.length, actual.length);
      }
   }
}
