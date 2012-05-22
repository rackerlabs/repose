package com.rackspace.papi.commons.util.io;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Enclosed.class)
public class FilePathReaderImplTest {

   public static class WhenLoadingValidResources {
      private static final String FILE_NAME = "/test.properties";
      private FilePathReaderImpl reader;
      
      @Before
      public void setUp() {
         reader = new FilePathReaderImpl(FILE_NAME);
      }

      @Test 
      public void shouldPassPreconditionsForValidResource() throws IOException {
         reader.checkPreconditions();
      }
      
      @Test
      public void shouldLoadResourceAsStream() {
         InputStream stream = reader.getResourceAsStream();
         
         assertNotNull(stream);
      }
      
      @Test
      public void shouldLoadCorrectResource() throws IOException {
         final String expected = "somevalue";
         InputStream stream = reader.getResourceAsStream();

         Properties props = new Properties();
         props.load(stream);
         
         assertEquals(expected, props.get("somekey"));
      }
      
      @Test
      public void shouldGetReader() throws IOException {
         BufferedReader bufferedReader = reader.getReader();
         assertNotNull(bufferedReader);
      }
      
      @Test
      public void shouldReadEntireFile() throws IOException {
         String expected = "somekey=somevalue";
         String data = reader.read();
         
         assertEquals(expected, data);
      }
      
   }

   public static class WhenLoadingInvalidResources {
      private static final String INVALID_FILE_NAME = "/blah.properties";
      private static final String EMPTY_FILE_NAME = "/empty.txt";
      private FilePathReaderImpl badReader;
      private FilePathReaderImpl emptyReader;
      
      @Before
      public void setUp() {
         badReader = new FilePathReaderImpl(INVALID_FILE_NAME);
         emptyReader = new FilePathReaderImpl(EMPTY_FILE_NAME);
      }

      @Test(expected=FileNotFoundException.class)
      public void shouldThrowExceptionForInvalidResource() throws IOException {
         badReader.checkPreconditions();
      }
      
      @Test(expected=FileNotFoundException.class)
      public void shouldThrowExceptionForEmptyResource() throws IOException {
         emptyReader.checkPreconditions();
      }
      
   }
}
