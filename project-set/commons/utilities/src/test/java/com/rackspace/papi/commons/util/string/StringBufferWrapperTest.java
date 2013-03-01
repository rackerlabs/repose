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
public class StringBufferWrapperTest {
    
    static StringBufferWrapper instance;
    
      @BeforeClass
    public static void setUpClass() {
         instance =new StringBufferWrapper(new StringBuffer("test"));
    }
    
    /**
     * Test of indexOf method, of class StringBufferWrapper.
     */
    @Test
    public void testIndexOf_String() {
        
        String seq = "st";
       
        int expResult = 2;
        int result = instance.indexOf(seq);
        assertEquals(expResult, result);
        
    }

    /**
     * Test of indexOf method, of class StringBufferWrapper.
     */
    @Test
    public void testIndexOf_String_int() {
       
        String seq = "t";
        int fromIndex = 2;
        
        int expResult = 3;
        int result = instance.indexOf(seq, fromIndex);
        assertEquals(expResult, result);
        
    }

    /**
     * Test of asCharSequence method, of class StringBufferWrapper.
     */
    @Test
    public void testAsCharSequence() {
 
       
        CharSequence result = instance.asCharSequence();
        assertEquals("test", result.toString());
      
    }

    /**
     * Test of charAt method, of class StringBufferWrapper.
     */
    @Test
    public void testCharAt() {
        
        int i = 0;
       
        char expResult = 't';
        char result = instance.charAt(i);
        assertEquals(expResult, result);
        
    }

    /**
     * Test of length method, of class StringBufferWrapper.
     */
    @Test
    public void testLength() {
     
        int expResult = 4;
        int result = instance.length();
        assertEquals(expResult, result);
        
    }

    /**
     * Test of subSequence method, of class StringBufferWrapper.
     */
    @Test
    public void testSubSequence() {
       
        int i = 1;
        int i1 = 3;
       
     
        CharSequence result = instance.subSequence(i, i1);
        assertEquals("es", result.toString());
       
    }
}
