package com.rackspace.papi.commons.util;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 *
 *
 */
@RunWith(Enclosed.class)
public class StringUriUtilitiesTest {

   public static class WhenIdentifyingUriFragments {

      @Test
      public void shouldIdentifyRootFragments() {
         assertEquals(0, StringUriUtilities.indexOfUriFragment("/v1", "/v1"));
         assertEquals(0, StringUriUtilities.indexOfUriFragment("/v1/", "/v1"));
      }

      @Test
      public void shouldIdentifyPrependedFragments() {
         assertEquals(0, StringUriUtilities.indexOfUriFragment("/v1/requested/uri", "/v1"));
      }

      @Test
      public void shouldIdentifyEmbeddedFragments() {
         assertEquals(10, StringUriUtilities.indexOfUriFragment("/versioned/v1/requested/uri", "/v1"));
      }

      @Test
      public void shouldIdentifyAppendedFragments() {
         assertEquals(24, StringUriUtilities.indexOfUriFragment("/requested/uri/versioned/v1", "/v1"));
      }

      @Test
      public void shouldNotIdentifyPartiallyMatchingEmbeddedFragments() {
         assertEquals(-1, StringUriUtilities.indexOfUriFragment("/versioned/v12/requested/uri", "/v1"));
      }
   }

   public static class WhenFormattingURIs {

      @Test
      public void shouldAddRootReference() {
         assertEquals("Should add a root reference to a URI", "/a/resource", StringUriUtilities.formatUri("a/resource"));
      }

      @Test
      public void shouldRemoveTrailingSlash() {
         assertEquals("Should remove trailing slashes from a URI", "/a/resource", StringUriUtilities.formatUri("/a/resource/"));
      }

      @Test
      public void shouldRemovingExtraLeadingSlashes() {
         assertEquals("Should remove multiple leading slasshes from a URI", "/a/resource", StringUriUtilities.formatUri("//////////a/resource///"));
      }

      @Test
      public void shouldReturnRootContextURI() {
         assertEquals("Should not return an empty string when passed a root context URI", "/", StringUriUtilities.formatUri("/"));
      }

      @Test
      public void shouldReturnRootContextURI2() {
         assertEquals("Should not return an empty string when passed a root context URI", "/", StringUriUtilities.formatUri("/////////"));
      }
      
      @Test
      public void shouldReturnRootContextURI3(){
          assertEquals("Should not return an empty string when passed a root context URI", "/", StringUriUtilities.formatUri(""));
      }
   }

   public static class WhenConcatenatingUris {

      @Test
      public void shouldAddLeadingSlash() {
         String uri1 = "one/two";
         String uri2 = "three/four";
         String expected = "/" + uri1 + "/" + uri2;
         
         String actual = StringUriUtilities.concatUris(uri1, uri2);
         
         assertEquals(expected, actual);

      }

      @Test
      public void shouldNotRemoveExtraSlash() {
         String uri1 = "one/two/";
         String uri2 = "/three/four/";
         String expected = "/one/two//three/four/";
         
         String actual = StringUriUtilities.concatUris(uri1, uri2);
         
         assertEquals(expected, actual);

      }
      
      @Test
      public void shouldHandleOneString() {
         String uri1 = "one/two/";
         String expected = "/one/two/";
         
         String actual = StringUriUtilities.concatUris(uri1);
         
         assertEquals(expected, actual);

      }

      @Test
      public void shouldSkipEmptyStrings() {
         String uri1 = "one/two/";
         String uri2 = "/three/four/";
         String expected = "/one/two//three/four/";
         
         String actual = StringUriUtilities.concatUris("", "    ", uri1, " ", "", uri2, "");
         
         assertEquals(expected, actual);

      }
      
      @Test
      public void shouldHandleSingle() {
         String uri1 = "/";
         String uri2 = "/";
         String expected = "//";
         
         String actual = StringUriUtilities.concatUris(uri1, uri2);
         
         assertEquals(expected, actual);

      }
      
   }
   
   public static class WhenEncodingUris{
      
      @Test
      public void shouldNotChangeWhenEncodingNonEncodableCharacters(){
         String uri1 = "qwerasdfjklvcxhjkfe-3djfkdfs";
         
         String uri2 = StringUriUtilities.encodeUri(uri1);
         
         assertEquals(uri2, uri1);
      }
      
      @Test
      public void shouldChangeWhenEncodingEncodableCharacters(){
         String uri1 = "key$test";
         
         String uri2 = StringUriUtilities.encodeUri(uri1);
         
         assertEquals(uri2, "key%24test");
      }
      
   }
}
