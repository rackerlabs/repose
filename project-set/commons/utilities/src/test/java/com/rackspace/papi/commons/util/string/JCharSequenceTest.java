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
public class JCharSequenceTest {
    

    /**
     * Test of asCharSequence method, of class JCharSequence.
     */
    @Test
    public void testAsCharSequence() {
       
        JCharSequence instance = new StringWrapper("test");
        CharSequence result = instance.asCharSequence();
        assertEquals("test", result.toString());
       
    }

    /**
     * Test of indexOf method, of class JCharSequence.
     */
    @Test
    public void testIndexOf_String() {
        System.out.println("indexOf");
        String seq = "st";
        JCharSequence instance = new StringWrapper("test");
      
        int result = instance.indexOf(seq);
        assertEquals(2, result);
      
    }

    /**
     * Test of indexOf method, of class JCharSequence.
     */
    @Test
    public void testIndexOf_String_int() {
        
        String seq = "st";
        int fromIndex = 4;
        JCharSequence instance = new StringWrapper("testtest");
        int expResult = 0;
        int result = instance.indexOf(seq, fromIndex);
        assertEquals(6, result);
       
    }


}
