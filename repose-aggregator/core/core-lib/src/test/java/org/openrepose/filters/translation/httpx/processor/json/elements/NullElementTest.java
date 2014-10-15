/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openrepose.filters.translation.httpx.processor.json.elements;

import org.junit.Test;
import org.xml.sax.ContentHandler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 *
 * @author kush5342
 */
public class NullElementTest {
    
    /**
     * Test of outputElement method, of class NullElement.
     */
    @Test
    public void testOutputElement() throws Exception {
        ContentHandler handler = mock(ContentHandler.class);
        NullElement instance = new NullElement(BaseElement.JSONX_URI,"fid","value");
        instance.outputElement(handler);
          assertEquals("fid", instance.getAttributes().getValue(0));
    }
}
