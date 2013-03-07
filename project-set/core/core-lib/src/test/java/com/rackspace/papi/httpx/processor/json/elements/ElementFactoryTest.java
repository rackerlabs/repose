/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.httpx.processor.json.elements;

import com.rackspace.papi.httpx.processor.common.Element;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kush5342
 */
public class ElementFactoryTest {
    

     private static final Logger LOG = LoggerFactory.getLogger(ElementFactory.class);
    /**
     * Test of getElement method, of class ElementFactory.
     */
    @Test
    public void testGetElement() {

        String tokenName = "START_OBJECT";
        String name = "fid";
        Element result = ElementFactory.getElement(tokenName, name);
         assertNotNull(result);
       
     
    }

    /**
     * Test of getScalarElement method, of class ElementFactory.
     */
    @Test
    public void testGetScalarElement() {
   
     
        String tokenName = "VALUE_STRING";
        String name = "fid";
        Object value = "value";
        Element expResult = new ScalarElement<String>("VALUE_STRING", "fid", "value");
        Element result = ElementFactory.getScalarElement(tokenName, name, value);
        assertNotNull(result);
   
    }
}
