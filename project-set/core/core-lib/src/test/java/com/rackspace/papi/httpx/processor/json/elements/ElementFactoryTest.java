/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.httpx.processor.json.elements;

import com.rackspace.papi.httpx.processor.common.Element;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kush5342
 */
public class ElementFactoryTest {
    

    /**
     * Test of getElement method, of class ElementFactory.
     */
    @Test
    public void testGetElement() {
        System.out.println("getElement");
        ElementFactory.getElement("nid","fid");
        String tokenName = "nid";
        String name = "fid";
       
      
        //Element expResult = (Element)ElementFactory.VALUE_STRING.getClass().getConstructors()[0].newInstance("nid","fid");
        Element result = ElementFactory.getElement(tokenName, name);
        //assertEquals(expResult, result);
     
    }

    /**
     * Test of getScalarElement method, of class ElementFactory.
     */
    @Test
    public void testGetScalarElement() {
        System.out.println("getScalarElement");
        ElementFactory.getScalarElement("nid","fid", "value");
        String tokenName = "nid";
        String name = "fid";
        Object value = "value";
        Element expResult = new ScalarElement<String>("nid", "fid", "value");
        Element result = ElementFactory.getScalarElement(tokenName, name, value);
        //assertEquals(expResult, result);
   
    }
}
