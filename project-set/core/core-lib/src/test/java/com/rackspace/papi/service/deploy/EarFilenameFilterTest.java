/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.service.deploy;

import java.io.File;
import java.io.FilenameFilter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;


@RunWith(Enclosed.class)
public class EarFilenameFilterTest {
   
   public static class WhenLocatingEarFile{
      
      protected File dir = new File("/usr/share/repose/filters");
      protected EarFilenameFilter earFilenameFilter;
      
      @Before
      public void setUp(){
         
         earFilenameFilter = (EarFilenameFilter)EarFilenameFilter.getInstance();
      }
      
      @Test
      public void shouldReturnTrueForValidEarName(){
         
         assertTrue(earFilenameFilter.accept(dir, "filter-bundle.ear"));
      }
      
      @Test
      public void shouldReturnFalseForInvalidEarName(){
         assertFalse(earFilenameFilter.accept(dir, "filter-bunder"));
      }
      
      @Test
      public void shouldReturnFalseForEmptyEarName(){
         assertFalse(earFilenameFilter.accept(dir, ""));
      }
   }
}
