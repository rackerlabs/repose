/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.httpx.processor.common;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

/**
 *
 * @author kush5342
 */
@RunWith(Enclosed.class)
public class PreProcessorExceptionTest {
    
 public static class WhenException {
 
  @Test
        public void shouldProcessCustomMessage() {
            String expectedExceptionMessage = "Oops!  Something unexpected happened.";
             
            PreProcessorException preProcessorException =new PreProcessorException(expectedExceptionMessage);
             
            assertEquals(expectedExceptionMessage,preProcessorException.getMessage());
            
            String expectedExceptionMessage2 = "Oops!  Something unexpected happened again.";
            
            preProcessorException = new PreProcessorException(expectedExceptionMessage, new Throwable("unexpected"));

            assertEquals(expectedExceptionMessage, preProcessorException.getMessage());
           
   
             preProcessorException = new PreProcessorException(new Throwable("unexpected again"));
             
             assertEquals("unexpected again", preProcessorException.getCause().getMessage());
     
 }
 }
}
