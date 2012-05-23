package com.rackspace.papi.commons.util.regex;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class RegexSelectorTest {

   @Ignore
   public static class TestParent {

      protected RegexSelector<String> selector;

      @Before
      public final void beforeAll() {
         selector = new RegexSelector<String>();
         selector.addPattern("\\d\\d\\d[+-]", "notExpected");
         selector.addPattern("[+-]\\d\\d\\d", "expected");
      }
   }

   public static class WhenSelecting extends TestParent {

      @Test
      public void shouldSelectOnRegexMatch() {
         final SelectorResult<String> foundResult = selector.select("-124");

         assertTrue("Selector must have a key.", foundResult.hasKey());
         assertEquals("Selector should select expected key.", "expected", foundResult.getKey());
      }

      @Test
      public void shouldReturnEmptyResultWithNoMatch() {
         final SelectorResult<String> emptyResult = selector.select("expected");

         assertFalse("Selector must not have a key.", emptyResult.hasKey());
         assertNull("Selector should select expected key.", emptyResult.getKey());
      }
   }

   public static class WhenClearing extends TestParent {

      @Test
      public void shouldClearStoredSelectors() {
         final SelectorResult<String> foundResult = selector.select("-124");

         assertTrue("Selector must have a key.", foundResult.hasKey());
         assertEquals("Selector should select expected key.", "expected", foundResult.getKey());

         selector.clear();

         final SelectorResult<String> emptySelection = selector.select("-124");

         assertFalse("Selector must not have a key.", emptySelection.hasKey());
         assertNull("Selector should select expected key.", emptySelection.getKey());
      }
   }
}
