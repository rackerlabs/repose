/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.cnorm;


import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;


public class ContentNormalizationHandlerFactoryTest {
    
    private ContentNormalizationHandlerFactory instance;
    @Before
    public void setUp() {
        instance = new ContentNormalizationHandlerFactory();
    }
    

    
    @Test
    public void shouldCreateNewConfigListener() {
        int expected = 1;
         assertEquals("Should have a config listener", expected, instance.getListeners().size());
    }

    
    @Test
    public void shouldCreateNewInstanceOfContentNormalizationHandler() {
        ContentNormalizationHandler handler = instance.buildHandler();
        assertNotNull("Instance of Content Normalization Handler should not be null", handler);
    }
}
