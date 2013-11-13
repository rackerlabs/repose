/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.httpx.processor.json.elements;


import org.junit.Test;
import static org.junit.Assert.*;
import org.xml.sax.ContentHandler;
import static org.mockito.Mockito.*;

/**
 *
 * @author kush5342
 */
public class ScalarElementTest {
    
  
    /**
     * Test of outputElement method, of class ScalarElement.
     */
    @Test
    public void testOutputElement() throws Exception {
        ContentHandler handler = mock(ContentHandler.class);
        ScalarElement instance = new ScalarElement(BaseElement.JSONX_URI,"fid","value");
        instance.outputElement(handler);
        assertEquals("fid", instance.getAttributes().getValue(0));
    }
}
