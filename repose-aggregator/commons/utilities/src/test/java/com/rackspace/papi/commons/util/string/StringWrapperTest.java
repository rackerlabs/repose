/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.commons.util.string;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

/**
 *
 * @author kush5342
 */
public class StringWrapperTest {
    
    static StringWrapper instance;
    
   @BeforeClass
    public static void setUpClass() {
         instance = new StringWrapper("test");
    }
    
    /**
     * Test of indexOf method, of class StringBufferWrapper.
     */
    @Test
    public void testIndexOf_String() {
        
        String seq = "st";
       
        int expResult = 2;
        int result = instance.indexOf(seq);
        assertThat(expResult, equalTo(result));
        
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
        assertThat(expResult, equalTo(result));
        
    }

    /**
     * Test of asCharSequence method, of class StringBufferWrapper.
     */
    @Test
    public void testAsCharSequence() {
 
       
        CharSequence result = instance.asCharSequence();
        assertThat("test", equalTo(result.toString()));
      
    }

    /**
     * Test of charAt method, of class StringBufferWrapper.
     */
    @Test
    public void testCharAt() {
        
        int i = 0;
       
        char expResult = 't';
        char result = instance.charAt(i);
        assertThat(expResult, equalTo(result));
        
    }

    /**
     * Test of length method, of class StringBufferWrapper.
     */
    @Test
    public void testLength() {
     
        int expResult = 4;
        int result = instance.length();
        assertThat(expResult, equalTo(result));
        
    }

    /**
     * Test of subSequence method, of class StringBufferWrapper.
     */
    @Test
    public void testSubSequence() {
       
        int i = 1;
        int i1 = 3;
       
     
        CharSequence result = instance.subSequence(i, i1);
        assertThat("es", equalTo(result.toString()));
       
    }

    /**
     * Test of equals method
     */
    @Test
    public void testEquals() {
        StringWrapper instance2 = new StringWrapper("test");
        assertThat(instance,equalTo(instance2));
    }

    /**
     * Test of equals expecting false
     */
    @Test
    public void testEquals2() {
        StringWrapper instance2 = new StringWrapper("test2");
        assertThat(instance,not(equalTo(instance2)));
    }
}
