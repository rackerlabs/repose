/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.httpx.processor.json.elements;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author kush5342
 */
public class BaseElementTest {
    
    static BaseElement instance;
    
    @BeforeClass
    public static void setUpClass() {
        
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute("", "date", "date", "java.lang.String", "25-dec-05");
        instance = new BaseElement("id:TestID",atts);
        
    }
    
    @AfterClass
    public static void tearDownClass() {
        
        instance=null;
    }

    /**
     * Test of getElement method, of class BaseElement.
     */
    @Test
    public void testGetElement() {
        String expResult = "id:TestID";
        String result = instance.getElement();
        assertEquals(expResult, result);
        
    }

    /**
     * Test of getLocalName method, of class BaseElement.
     */
    @Test
    public void testGetLocalName_0args() {
   
        String expResult = "TestID";
        String result = instance.getLocalName();
        assertEquals(expResult, result);
        
    }

    /**
     * Test of getQname method, of class BaseElement.
     */
    @Test
    public void testGetQname_0args() {
   
        String expResult = "json:TestID";
        String result = instance.getQname();
        assertEquals(expResult, result);
       
    }

    /**
     * Test of getAttributes method, of class BaseElement.
     */
    @Test
    public void testGetAttributes() {
       
        AttributesImpl expResult = new AttributesImpl();
        expResult.addAttribute("", "date", "date", "java.lang.String", "25-dec-05");
        AttributesImpl result = instance.getAttributes();
        assertEquals(expResult.getQName(1), result.getQName(1));
    
    }

    /**
     * Test of getLocalName method, of class BaseElement.
     */
    @Test
    public void testGetLocalName_String() {
       
        String name = "id:TestID";
        String expResult = "TestID";
        String result = BaseElement.getLocalName(name);
        assertEquals(expResult, result);
      
    }

    /**
     * Test of getQname method, of class BaseElement.
     */
    @Test
    public void testGetQname_String() {
    
        String name = "id:TestID";
        String expResult = "json:TestID";
        String result = BaseElement.getQname(name);
        assertEquals(expResult, result);
   
    }
}
