package com.rackspace.papi.commons.util.logging.apache;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(Enclosed.class)
public class LogArgumentGroupExtractorTest {

   public static class WhenParsingLoggingTemplates {

      @Test
      public void shouldExtractEscapedPercent() {
         final String template = "%%";
         final LogArgumentGroupExtractor expected = LogArgumentGroupExtractor.instance("", "", "", "", "%");
         final Matcher m = LogArgumentGroupExtractor.LOG_CONSTANTS.PATTERN.matcher(template);
         
         m.find();
         
         LogArgumentGroupExtractor extractor = new LogArgumentGroupExtractor(m);
         
         assertEquals(expected, extractor);
      }

      @Test
      public void shouldExtractVariables() {
         final String template = "%{SOMEVAR}i";
         final LogArgumentGroupExtractor expected = LogArgumentGroupExtractor.instance("", "", "SOMEVAR", "", "i");
         final Matcher m = LogArgumentGroupExtractor.LOG_CONSTANTS.PATTERN.matcher(template);
         
         m.find();
         
         LogArgumentGroupExtractor extractor = new LogArgumentGroupExtractor(m);
         
         assertEquals(expected, extractor);
      }

      @Test
      public void shouldExtractStatusCodes() {
         final String template = "%100,200,300{SOMEVAR}i";
         final LogArgumentGroupExtractor expected = LogArgumentGroupExtractor.instance("", "100,200,300", "SOMEVAR", "", "i");
         final Matcher m = LogArgumentGroupExtractor.LOG_CONSTANTS.PATTERN.matcher(template);
         
         m.find();
         
         LogArgumentGroupExtractor extractor = new LogArgumentGroupExtractor(m);
         
         assertEquals(expected, extractor);
      }

      @Test
      public void shouldExtractNegatedStatusCodes() {
         final String template = "%!100,200,300{SOMEVAR}i";
         final LogArgumentGroupExtractor expected = LogArgumentGroupExtractor.instance("", "!100,200,300", "SOMEVAR", "", "i");
         final Matcher m = LogArgumentGroupExtractor.LOG_CONSTANTS.PATTERN.matcher(template);
         
         m.find();
         
         LogArgumentGroupExtractor extractor = new LogArgumentGroupExtractor(m);
         
         assertEquals(expected, extractor);
      }

      @Test
      public void shouldExtractLifeCycleModifiers() {
         final String template = "%>!100,200,300{SOMEVAR}i";
         final LogArgumentGroupExtractor expected = LogArgumentGroupExtractor.instance(">", "!100,200,300", "SOMEVAR", "", "i");
         final Matcher m = LogArgumentGroupExtractor.LOG_CONSTANTS.PATTERN.matcher(template);
         
         m.find();
         
         LogArgumentGroupExtractor extractor = new LogArgumentGroupExtractor(m);
         
         assertEquals(expected, extractor);
         assertEquals(">", extractor.getLifeCycleModifier());
      }

      @Test
      public void shouldExtractFormats() {
         final String template = "%>!100,200,300{SOMEVAR format1,format2}i";
         final LogArgumentGroupExtractor expected = LogArgumentGroupExtractor.instance(">", "!100,200,300", "SOMEVAR", "format1,format2", "i");
         final Matcher m = LogArgumentGroupExtractor.LOG_CONSTANTS.PATTERN.matcher(template);
         
         m.find();
         
         LogArgumentGroupExtractor extractor = new LogArgumentGroupExtractor(m);
         
         assertEquals(expected, extractor);
         assertEquals(2, extractor.getArguments().size());
         assertEquals(">", extractor.getLifeCycleModifier());
      }

      @Test
      public void shouldExtractFormats2() {
         final String template = "%>!100,200,300{SOMEVAR format1 format2}i";
         final LogArgumentGroupExtractor expected = LogArgumentGroupExtractor.instance(">", "!100,200,300", "SOMEVAR", "format1,format2", "i");
         final Matcher m = LogArgumentGroupExtractor.LOG_CONSTANTS.PATTERN.matcher(template);
         
         m.find();
         
         LogArgumentGroupExtractor extractor = new LogArgumentGroupExtractor(m);
         
         assertEquals(expected, extractor);
         assertEquals(2, extractor.getArguments().size());
         assertEquals(">", extractor.getLifeCycleModifier());
      }

   }

   public static class WhenComparing {

      @Test
      public void shouldHaveSameHashCode1() {
         final LogArgumentGroupExtractor e1 = LogArgumentGroupExtractor.instance("", "!100,200,300", "SOMEVAR", "", "i");
         final LogArgumentGroupExtractor e2 = LogArgumentGroupExtractor.instance(null, "!100,200,300", "SOMEVAR", "", "i");
         
         assertEquals(e1.hashCode(), e2.hashCode());
      }
         
      @Test
      public void shouldHaveSameHashCode2() {
         final LogArgumentGroupExtractor e1 = LogArgumentGroupExtractor.instance("1", "", "SOMEVAR", "", "i");
         final LogArgumentGroupExtractor e2 = LogArgumentGroupExtractor.instance("1", null, "SOMEVAR", "", "i");
         
         assertEquals(e1.hashCode(), e2.hashCode());
      }
      @Test
      public void shouldHaveSameHashCode3() {
         final LogArgumentGroupExtractor e1 = LogArgumentGroupExtractor.instance("", "!100,200,300", "", "", "i");
         final LogArgumentGroupExtractor e2 = LogArgumentGroupExtractor.instance(null, "!100,200,300", null, "", "i");
         
         assertEquals(e1.hashCode(), e2.hashCode());
      }
      @Test
      public void shouldHaveSameHashCode4() {
         final LogArgumentGroupExtractor e1 = LogArgumentGroupExtractor.instance("", "!100,200,300", "SOMEVAR", "", "");
         final LogArgumentGroupExtractor e2 = LogArgumentGroupExtractor.instance(null, "!100,200,300", "SOMEVAR", "", null);
         
         assertEquals(e1.hashCode(), e2.hashCode());
      }

      @Test
      public void shouldBeEqual1() {
         final LogArgumentGroupExtractor e1 = LogArgumentGroupExtractor.instance("", "!100,200,300", "SOMEVAR", "", "i");
         final LogArgumentGroupExtractor e2 = LogArgumentGroupExtractor.instance("", "!100,200,300", "SOMEVAR", "", "i");
         
         assertEquals(e1, e2);
      }
         
      @Test
      public void shouldNotBeEqualWhenComparingNullToEmpty1() {
         final LogArgumentGroupExtractor e1 = LogArgumentGroupExtractor.instance("", "!100,200,300", "SOMEVAR", "", "i");
         final LogArgumentGroupExtractor e2 = LogArgumentGroupExtractor.instance(null, "!100,200,300", "SOMEVAR", "", "i");
         
         assertFalse(e1.equals(e2));
      }
         
      @Test
      public void shouldNotBeEqual1() {
         final LogArgumentGroupExtractor e1 = LogArgumentGroupExtractor.instance("", "!100,200,300", "SOMEVAR", "", "i");
         
         assertFalse(e1.equals(new Object()));
      }
   }
}
