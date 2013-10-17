/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.httpx.processor.json.elements;

import org.junit.Test;
import org.xml.sax.ContentHandler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;


/**
 *
 * @author kush5342
 */
public class EndElementTest {
    
   /**
     * Test of outputElement method, of class EndElement.
     */
    @Test
    public void testOutputElement() throws Exception {
     
        ContentHandler handler = mock(ContentHandler.class);
        EndElement instance = new EndElement(BaseElement.JSONX_URI,"nid");
        instance.outputElement(handler);
        assertEquals(BaseElement.JSONX_URI, instance.getElement());
        
    }
}
