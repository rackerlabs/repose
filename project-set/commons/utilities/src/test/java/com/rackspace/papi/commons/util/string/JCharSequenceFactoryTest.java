/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.commons.util.string;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kush5342
 */
public class JCharSequenceFactoryTest {
    
   

    /**
     * Test of jchars method, of class JCharSequenceFactory.
     */
    @Test
    public void testJchars_String() {
         String st = "test";
      
         JCharSequence result = JCharSequenceFactory.jchars(st);
         assertEquals("test", result.toString());
       
    }

    /**
     * Test of jchars method, of class JCharSequenceFactory.
     */
    @Test
    public void testJchars_StringBuffer() {
        
        StringBuffer sb = new StringBuffer("test");        
        JCharSequence result = JCharSequenceFactory.jchars(sb);
        assertEquals("test", result.asCharSequence().toString());
        
    }

    /**
     * Test of jchars method, of class JCharSequenceFactory.
     */
    @Test
    public void testJchars_StringBuilder() {
       
        StringBuilder sb = new StringBuilder("Test");
        JCharSequence expResult = null;
        JCharSequence result = JCharSequenceFactory.jchars(sb);
        assertEquals("Test", result.asCharSequence().toString());
        
    }
}
